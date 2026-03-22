CREATE TABLE IF NOT EXISTS profile (
    id bigserial PRIMARY KEY,
    uid varchar(64) NOT NULL,
    first_name varchar(64) NOT NULL,
    last_name varchar(64) NOT NULL,
    birth_day date,
    phone varchar(20),
    email varchar(100),
    country varchar(60),
    city varchar(100),
    objective text,
    summary text,
    large_photo varchar(255),
    small_photo varchar(255),
    info text,
    password varchar(255) NOT NULL,
    completed boolean NOT NULL,
    created timestamp without time zone NOT NULL DEFAULT now(),
    facebook varchar(255),
    linkedin varchar(255),
    github varchar(255),
    stackoverflow varchar(255),
    CONSTRAINT chk_profile_uid_format CHECK (uid ~ '^[a-z0-9_-]{3,64}$'),
    CONSTRAINT chk_profile_uid_lowercase CHECK (uid = lower(uid))
);

CREATE UNIQUE INDEX IF NOT EXISTS profile_uid_key ON profile(uid);
CREATE UNIQUE INDEX IF NOT EXISTS profile_email_key ON profile(email);
CREATE UNIQUE INDEX IF NOT EXISTS profile_phone_key ON profile(phone);

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

CREATE TABLE IF NOT EXISTS certificate (
    id bigserial PRIMARY KEY,
    id_profile bigint NOT NULL,
    name varchar(50) NOT NULL,
    large_url varchar(255) NOT NULL,
    small_url varchar(255) NOT NULL,
    issuer varchar(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS course (
    id bigserial PRIMARY KEY,
    id_profile bigint NOT NULL,
    name varchar(60) NOT NULL,
    school varchar(60) NOT NULL,
    finish_date date
);

CREATE TABLE IF NOT EXISTS education (
    id bigserial PRIMARY KEY,
    id_profile bigint NOT NULL,
    summary varchar(100) NOT NULL,
    begin_year integer NOT NULL,
    finish_year integer,
    university text NOT NULL,
    faculty varchar(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS language (
    id bigserial PRIMARY KEY,
    id_profile bigint NOT NULL,
    name varchar(30) NOT NULL,
    level varchar(18) NOT NULL,
    type varchar(7) NOT NULL DEFAULT 'all',
    CONSTRAINT language_level_check CHECK (level IN (
        'beginner','elementary','pre_intermediate','intermediate','upper_intermediate','advanced','proficiency'
    )),
    CONSTRAINT language_type_check CHECK (type IN ('all','spoken','writing'))
);

CREATE UNIQUE INDEX IF NOT EXISTS language_profile_name_type_key ON language(id_profile, name, type);

CREATE TABLE IF NOT EXISTS practic (
    id bigserial PRIMARY KEY,
    id_profile bigint NOT NULL,
    company varchar(100) NOT NULL,
    demo varchar(255),
    src varchar(255),
    job_position varchar(100) NOT NULL,
    responsibilities text NOT NULL,
    begin_date date NOT NULL,
    finish_date date
);

CREATE TABLE IF NOT EXISTS skill (
    id bigserial PRIMARY KEY,
    id_profile bigint NOT NULL,
    category varchar(50) NOT NULL,
    value text NOT NULL
);

CREATE TABLE IF NOT EXISTS profile_hobby (
    id_profile bigint NOT NULL,
    id_hobby bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS profile_connection (
    id bigserial PRIMARY KEY,
    pair_key varchar(64) NOT NULL,
    requester_id bigint NOT NULL,
    addressee_id bigint NOT NULL,
    status varchar(16) NOT NULL,
    created timestamptz NOT NULL DEFAULT now(),
    responded timestamptz,
    CONSTRAINT chk_profile_connection_self CHECK (requester_id <> addressee_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_profile_connection_pair ON profile_connection(pair_key);
CREATE INDEX IF NOT EXISTS idx_profile_connection_addressee_status ON profile_connection(addressee_id, status);
CREATE INDEX IF NOT EXISTS idx_profile_connection_requester_status ON profile_connection(requester_id, status);

ALTER TABLE certificate
    ADD CONSTRAINT certificate_fk FOREIGN KEY (id_profile) REFERENCES profile(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE course
    ADD CONSTRAINT course_fk FOREIGN KEY (id_profile) REFERENCES profile(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE education
    ADD CONSTRAINT education_fk FOREIGN KEY (id_profile) REFERENCES profile(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE language
    ADD CONSTRAINT language_fk FOREIGN KEY (id_profile) REFERENCES profile(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE practic
    ADD CONSTRAINT practic_fk FOREIGN KEY (id_profile) REFERENCES profile(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE skill
    ADD CONSTRAINT skill_fk FOREIGN KEY (id_profile) REFERENCES profile(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE profile_hobby
    ADD CONSTRAINT profile_hobby_profile_fk FOREIGN KEY (id_profile) REFERENCES profile(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE profile_hobby
    ADD CONSTRAINT profile_hobby_hobby_fk FOREIGN KEY (id_hobby) REFERENCES hobby(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE profile_connection
    ADD CONSTRAINT fk_profile_connection_addressee FOREIGN KEY (addressee_id) REFERENCES profile(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE profile_connection
    ADD CONSTRAINT fk_profile_connection_requester FOREIGN KEY (requester_id) REFERENCES profile(id) ON UPDATE CASCADE ON DELETE CASCADE;

CREATE UNIQUE INDEX IF NOT EXISTS certificate_unique_profile_name_issuer_idx ON certificate (
    id_profile,
    lower(regexp_replace(trim(both from name), '\\s+', ' ', 'g')),
    lower(regexp_replace(trim(both from issuer), '\\s+', ' ', 'g'))
);

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
