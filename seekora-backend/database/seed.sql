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
 (1, 'Knowledge base search', 1),
 (1, '10 searches per day', 2),
 (1, 'Source links included', 3),
 (1, 'Web application access', 4),
 (2, 'Unlimited knowledge base searches', 1),
 (2, 'Stored topic summaries', 2),
 (2, 'Saved search history', 3),
 (2, 'Profile management', 4),
 (2, 'Browser voice input', 5),
 (2, 'JSON API endpoints', 6);
-- New users create accounts through the signup endpoint.

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
  'This demo includes Freemium and Premium plan records. Premium plan selection is a demonstration only; no payment is processed.', 1),
 ('FEATURES', 'feature,what can you do,summary,source,image',
  'Seekora searches a local knowledge base, displays saved topic summaries and source links, and supports accounts, search history and voice input.', 2),
 ('API', 'api,developer,integrate,rest',
  'Seekora exposes JSON API endpoints for its current features. There is no external AI provider integration configured.', 3),
 ('ACCOUNT', 'account,sign up,signup,login,register,password',
  'Click Get Started to create a free account. You can log in any time to see your search history.', 4),
 ('CONTACT', 'contact,support,help,email',
  'Use the contact form to save a message for the site administrator.', 5),
 ('GREETING', 'hi,hello,hey,namaste',
  'Hi! I am the Seekora demo assistant. Ask about the knowledge base, accounts, or demo plans.', 6);
-- ---------------------------- marketing ------------------------------
INSERT INTO features (icon, title, description, sort_order) VALUES
 ('*', 'Knowledge Base Search', 'Search the locally maintained Seekora topic collection from one simple interface.', 1),
 ('*', 'Stored Topic Summaries', 'Read concise summaries saved with supported knowledge base topics.', 2),
 ('*', 'Topic Source Links', 'Open the source links stored with a matching topic to continue your research.', 3),
 ('*', 'Search History', 'Signed-in users can revisit and manage their previous searches.', 4),
 ('*', 'Voice Input', 'Use browser speech recognition to enter a search question where supported.', 5),
 ('*', 'Account Profiles', 'Create an account to keep search history and manage your profile.', 6);

INSERT INTO faqs (question, answer, sort_order) VALUES
 ('What does Seekora search?', 'The current demo searches its local knowledge base, which contains a small collection of seeded topics and source links. It does not search the live web.', 1),
 ('Are summaries generated by an AI provider?', 'No external AI provider is configured. Seekora displays summaries stored with matching knowledge base topics.', 2),
 ('Can I save my searches?', 'Signed-in users can view and manage their search history. Guest searches are associated with the current browser session.', 3),
 ('Does Premium process a payment?', 'No. Plan selection is available to demonstrate the subscription flow; no payment gateway is connected.', 4);

INSERT INTO platform_stats (label, value, suffix, sort_order) VALUES
 ('Seeded Topics', 5, '', 1),
 ('Guest Search Limit', 10, '/day', 2),
 ('Seeded Source Links', 9, '', 3),
 ('Chat Reply Intents', 6, '', 4);
