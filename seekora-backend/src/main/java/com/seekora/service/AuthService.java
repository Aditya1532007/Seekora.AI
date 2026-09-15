package com.seekora.service;

import com.seekora.dao.ActivityLogDao;
import com.seekora.dao.PlanDao;
import com.seekora.dao.UserDao;
import com.seekora.model.Plan;
import com.seekora.model.User;
import com.seekora.util.PasswordUtil;
import com.seekora.util.Validator;

import java.util.Optional;

public class AuthService {

    private static final String DEFAULT_PLAN = "FREEMIUM";

    private final UserDao userDao = new UserDao();
    private final PlanDao planDao = new PlanDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public User signup(String fullName, String email, String rawPassword, String ip) {
        if (Validator.isBlank(fullName)) {
            throw new ServiceException("Full name is required");
        }
        if (!Validator.isEmail(email)) {
            throw new ServiceException("A valid email address is required");
        }
        if (!Validator.isStrongEnough(rawPassword)) {
            throw new ServiceException("Password must be at least 6 characters long");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (userDao.emailExists(normalizedEmail)) {
            throw new ServiceException("An account with this email already exists", 409);
        }
        Plan plan = planDao.findByCode(DEFAULT_PLAN)
                .orElseThrow(() -> new ServiceException("Default plan is missing - did you run seed.sql?", 500));

        long id = userDao.create(Validator.truncate(fullName.trim(), 120), normalizedEmail,
                PasswordUtil.hash(rawPassword), plan.getId());
        activityLogDao.log(id, "SIGNUP", "Account created", ip);
        return userDao.findById(id).orElseThrow(() -> new ServiceException("User could not be loaded", 500));
    }

    public User login(String email, String rawPassword, String ip) {
        if (Validator.isBlank(email) || Validator.isBlank(rawPassword)) {
            throw new ServiceException("Email and password are required");
        }
        Optional<User> found = userDao.findByEmail(email.trim().toLowerCase());
        User user = found.orElseThrow(() -> new ServiceException("Invalid email or password", 401));
        if (!PasswordUtil.matches(rawPassword, user.getPasswordHash())) {
            throw new ServiceException("Invalid email or password", 401);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ServiceException("This account is not active", 403);
        }
        userDao.touchLastLogin(user.getId());
        activityLogDao.log(user.getId(), "LOGIN", "Signed in", ip);
        return user;
    }

    public User profile(long userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new ServiceException("User not found", 404));
    }

    public User updateProfile(long userId, String fullName) {
        if (Validator.isBlank(fullName)) {
            throw new ServiceException("Full name is required");
        }
        userDao.updateProfile(userId, Validator.truncate(fullName.trim(), 120));
        return profile(userId);
    }

    public void changePassword(long userId, String currentPassword, String newPassword) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ServiceException("User not found", 404));
        if (!PasswordUtil.matches(currentPassword, user.getPasswordHash())) {
            throw new ServiceException("Current password is incorrect", 401);
        }
        if (!Validator.isStrongEnough(newPassword)) {
            throw new ServiceException("New password must be at least 6 characters long");
        }
        userDao.updatePassword(userId, PasswordUtil.hash(newPassword));
    }
}
