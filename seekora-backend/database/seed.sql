-- =====================================================================
--  Seekora - demo / starter data
--  Run after schema.sql:  mysql -u root -p < database/seed.sql
-- =====================================================================
USE seekora_db;

-- ------------------------------ plans --------------------------------
INSERT INTO plans (code, name, price_monthly, price_yearly, daily_search_limit, is_featured, sort_order) VALUES
 ('FREEMIUM', 'Freemium', 0.00,  0.00,  10, 0, 1),
 ('PREMIUM',  'Premium',  9.00, 86.40,  -1, 1, 2);

INSERT INTO plan_features (plan_id, feature, sort_order) VALUES
 (1, 'AI-powered search (basic)', 1),
 (1, 'Daily summary limits', 2),
 (1, 'Source links included', 3),
 (1, 'Web application access', 4),
 (2, 'Unlimited AI searches', 1),
 (2, 'Advanced summaries & analysis', 2),
 (2, 'PDF upload & analysis', 3),
 (2, 'Search history & personalization', 4),
 (2, 'Priority customer support', 5),
 (2, 'API access for developers', 6);

-- ------------------------------ users --------------------------------
-- password for both demo accounts: Passw0rd!
INSERT INTO users (full_name, email, password_hash, role, plan_id) VALUES
 ('Platform Admin', 'admin@seekora.com', '$2a$10$ESURwsS9eKPq3rYX2MGZqeB65IUdUisedo4ndf6nNpkw4qKWlXz1O', 'ADMIN', 2),
 ('Demo Student',   'demo@seekora.com',  '$2a$10$ESURwsS9eKPq3rYX2MGZqeB65IUdUisedo4ndf6nNpkw4qKWlXz1O', 'USER',  1);

INSERT INTO subscriptions (user_id, plan_id, billing_cycle, amount, status, expires_at) VALUES
 (1, 2, 'YEARLY', 86.40, 'ACTIVE', DATE_ADD(NOW(), INTERVAL 1 YEAR));

-- -------------------------- knowledge base ---------------------------
INSERT INTO knowledge_base (topic, keywords, summary, accuracy) VALUES
 ('Quantum Computing', 'quantum,qubit,superposition',
  'Quantum computing uses "qubits" that can be 0 and 1 at the same time (superposition). This lets quantum computers solve certain problems - like drug discovery and cryptography - millions of times faster than normal computers.', 98),
 ('Photosynthesis', 'photosynthesis,chlorophyll,plants',
  'Photosynthesis is how plants make food: they capture sunlight with chlorophyll, absorb CO2 from the air and water from the roots, and convert them into glucose (energy) and oxygen.', 97),
 ('Artificial Intelligence', 'ai,artificial intelligence,machine learning,neural network',
  'AI works by training models on huge amounts of data so they learn patterns. When you ask something, the model predicts the most useful answer based on what it learned.', 96),
 ('Climate Change', 'climate,global warming,greenhouse',
  'Climate change is the long-term warming of Earth caused mainly by greenhouse gases (CO2, methane) from burning fossil fuels. Effects include rising seas, extreme weather and ecosystem disruption.', 97),
 ('Blockchain', 'blockchain,crypto,ledger,bitcoin',
  'Blockchain is a shared, unchangeable digital ledger. Transactions are grouped into "blocks" and cryptographically linked into a "chain", making data transparent and tamper-proof.', 95);

INSERT INTO knowledge_sources (knowledge_id, title, url, source_type) VALUES
 (1, 'Wikipedia - Quantum computing', 'https://en.wikipedia.org/wiki/Quantum_computing', 'ARTICLE'),
 (1, 'Google Scholar - Quantum algorithms', 'https://scholar.google.com/scholar?q=quantum+algorithms', 'PAPER'),
 (2, 'Wikipedia - Photosynthesis', 'https://en.wikipedia.org/wiki/Photosynthesis', 'ARTICLE'),
 (2, 'Khan Academy - Photosynthesis', 'https://www.khanacademy.org/science/biology/photosynthesis-in-plants', 'VIDEO'),
 (3, 'Wikipedia - Artificial intelligence', 'https://en.wikipedia.org/wiki/Artificial_intelligence', 'ARTICLE'),
 (3, 'Google Scholar - Deep learning', 'https://scholar.google.com/scholar?q=deep+learning', 'PAPER'),
 (4, 'Wikipedia - Climate change', 'https://en.wikipedia.org/wiki/Climate_change', 'ARTICLE'),
 (4, 'NASA - Climate evidence', 'https://climate.nasa.gov/evidence/', 'ARTICLE'),
 (5, 'Wikipedia - Blockchain', 'https://en.wikipedia.org/wiki/Blockchain', 'ARTICLE');

-- ----------------------------- chat bot ------------------------------
INSERT INTO chat_intents (intent, keywords, reply, sort_order) VALUES
 ('PRICING', 'price,pricing,cost,plan,premium,free',
  'We have two plans: Freemium ($0/month, 10 searches a day) and Premium ($9/month or $86.40/year) with unlimited searches, PDF analysis and API access.', 1),
 ('FEATURES', 'feature,what can you do,summary,source,image',
  'Seekora gives you AI summaries in simple words, verified source links, related images, search history and a developer API.', 2),
 ('API', 'api,developer,integrate,rest',
  'Premium users get REST API access - every endpoint returns JSON so you can embed AI search into your own app.', 3),
 ('ACCOUNT', 'account,sign up,signup,login,register,password',
  'Click "Get Started" to create a free account. You can log in any time to see your search history.', 4),
 ('CONTACT', 'contact,support,help,email',
  'Send us a message through the contact form and our team will reply within 24 hours.', 5),
 ('GREETING', 'hi,hello,hey,namaste',
  'Hi! I am the Seekora assistant. Ask me about features, pricing, the API or your account.', 6);

-- ---------------------------- marketing ------------------------------
INSERT INTO features (icon, title, description, sort_order) VALUES
 ('*', 'All-in-One Platform', 'One platform for all information needs - no more jumping between dozens of scattered websites.', 1),
 ('*', 'AI-Generated Summaries', 'Complex content simplified into simple words, so anyone can understand any topic instantly.', 2),
 ('*', 'Reliable Source Links', 'Every answer comes with transparent source links so you can verify and trust the information.', 3),
 ('*', 'Related Images', 'Visual learning made easy with related images for better understanding of every topic.', 4),
 ('*', 'Save Time & Effort', 'Stop wasting hours searching. Get accurate answers in seconds, not scattered across tabs.', 5),
 ('*', 'Accurate & Easy to Use', 'Fast, accurate results wrapped in an intuitive, user-friendly interface for everyone.', 6);

INSERT INTO faqs (question, answer, sort_order) VALUES
 ('Is the platform really free to use?', 'Yes! Our Freemium plan gives you AI-powered search, summaries and source links at no cost. Upgrade to Premium for unlimited searches, PDF analysis and API access.', 1),
 ('How accurate are the AI summaries?', 'Every summary is generated by leading AI models and paired with transparent, verified source links so you can always double-check the information yourself.', 2),
 ('Can developers integrate it into their own apps?', 'Absolutely. Premium users get API access with REST endpoints, making it easy to embed AI search and summarization into any product.', 3),
 ('Do you partner with educational institutions?', 'Yes - we offer institutional and educational partnerships with custom plans for schools, colleges and universities.', 4);

INSERT INTO testimonials (author_name, author_title, initials, rating, content) VALUES
 ('Ananya R.', 'College Student',      'AR', 5, 'I finished my research paper in half the time. The source links make citations effortless!'),
 ('David K.',  'Educator',             'DK', 5, 'The simplified summaries help me explain tough topics to my class. A must-have for teachers.'),
 ('Sara M.',   'Software Professional','SM', 5, 'Fast, accurate, and the API access lets our team integrate it into our own tools. Brilliant.');

INSERT INTO platform_stats (label, value, suffix, sort_order) VALUES
 ('Faster Research', 10, 'x',  1),
 ('Verified Sources', 100, '%', 2),
 ('Active Learners', 50, 'K+', 3),
 ('User Segments', 6, '+', 4);
