CREATE TABLE IF NOT EXISTS hobby (
    id bigserial PRIMARY KEY,
    name varchar(30) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS hobby_name_unique ON hobby(name);

CREATE TABLE IF NOT EXISTS skill_category (
    id bigserial PRIMARY KEY,
    category varchar(50) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS skill_category_category_key ON skill_category(category);

INSERT INTO hobby (id, name) VALUES
    (1, 'Cycling'),
    (2, 'Handball'),
    (3, 'Football'),
    (4, 'Basketball'),
    (5, 'Bowling'),
    (6, 'Boxing'),
    (7, 'Volleyball'),
    (8, 'Baseball'),
    (9, 'Skating'),
    (10, 'Skiing'),
    (11, 'Table tennis'),
    (12, 'Tennis'),
    (13, 'Weightlifting'),
    (14, 'Automobiles'),
    (15, 'Book reading'),
    (16, 'Cricket'),
    (17, 'Photo'),
    (18, 'Shopping'),
    (19, 'Cooking'),
    (20, 'Codding'),
    (21, 'Animals'),
    (22, 'Traveling'),
    (23, 'Movie'),
    (24, 'Painting'),
    (25, 'Darts'),
    (26, 'Fishing'),
    (27, 'Kayak slalom'),
    (28, 'Games of chance'),
    (29, 'Ice hockey'),
    (30, 'Roller skating'),
    (31, 'Swimming'),
    (32, 'Diving'),
    (33, 'Golf'),
    (34, 'Shooting'),
    (35, 'Rowing'),
    (36, 'Camping'),
    (37, 'Archery'),
    (38, 'Pubs'),
    (39, 'Music'),
    (40, 'Computer games'),
    (41, 'Authorship'),
    (42, 'Singing'),
    (43, 'Foreign lang'),
    (44, 'Billiards'),
    (45, 'Skateboarding'),
    (46, 'Collecting'),
    (47, 'Badminton'),
    (48, 'Disco')
ON CONFLICT DO NOTHING;

INSERT INTO skill_category (id, category) VALUES
    (1, 'Languages'),
    (2, 'DBMS'),
    (3, 'Web'),
    (4, 'Java'),
    (5, 'IDE'),
    (6, 'CVS'),
    (7, 'Web Servers'),
    (8, 'Build system'),
    (9, 'Cloud'),
    (10, 'Frameworks'),
    (11, 'Tools'),
    (12, 'Testing'),
    (13, 'Other')
ON CONFLICT DO NOTHING;
