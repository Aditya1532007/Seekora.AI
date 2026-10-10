package com.seekora.service;

import com.seekora.dao.ActivityLogDao;
import com.seekora.dao.PlanDao;
import com.seekora.dao.SubscriptionDao;
import com.seekora.model.Plan;
import com.seekora.model.Subscription;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class SubscriptionService {

    private final PlanDao planDao = new PlanDao();
    private final SubscriptionDao subscriptionDao = new SubscriptionDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public List<Plan> plans() {
        List<Plan> plans = planDao.findAllWithFeatures();
        for (Plan plan : plans) {
            if ("FREEMIUM".equals(plan.getCode())) {
                plan.setFeatures(Arrays.asList("Knowledge base search", "10 searches per day",
                        "Source links included", "Web application access"));
            } else if ("PREMIUM".equals(plan.getCode())) {
                plan.setFeatures(Arrays.asList("Unlimited knowledge base searches", "Stored topic summaries",
                        "Saved search history", "Profile management", "Browser voice input",
                        "JSON API endpoints"));
            }
        }
        return plans;
    }

    public Subscription subscribe(long userId, String planCode, String billingCycle, String ip) {
        if (planCode == null || planCode.trim().isEmpty()) {
            throw new ServiceException("Plan code is required");
        }
        String cycle = billingCycle == null || billingCycle.trim().isEmpty() ? "MONTHLY" : billingCycle.toUpperCase();
        if (!"MONTHLY".equals(cycle) && !"YEARLY".equals(cycle)) {
            throw new ServiceException("Billing cycle must be MONTHLY or YEARLY");
        }
        Plan plan = planDao.findByCode(planCode.trim().toUpperCase())
                .orElseThrow(() -> new ServiceException("Unknown plan: " + planCode, 404));

        BigDecimal amount = "YEARLY".equals(cycle) ? plan.getPriceYearly() : plan.getPriceMonthly();
        long id = subscriptionDao.subscribe(userId, plan.getId(), cycle, amount);
        activityLogDao.log(userId, "SUBSCRIBE", plan.getCode() + " / " + cycle, ip);

        return subscriptionDao.findByUser(userId).stream()
                .filter(s -> s.getId() == id)
                .findFirst()
                .orElseThrow(() -> new ServiceException("Subscription could not be loaded", 500));
    }

    public List<Subscription> history(long userId) {
        return subscriptionDao.findByUser(userId);
    }
}
