create database if not exists trademart;

use trademart;

create table if not exists users (
    user_id int primary key,
    name varchar(256),
    email varchar(256),
    password varchar(256),
    password_salt varchar(256),
    prefered_posts varchar(128),
    verified bit
);

create table if not exists skill_cards (
    skill_card_id int primary key,
    skill_title varchar(256),
    skill_description varchar(1024),
    tags varchar(1024),
    video_url varchar(2000),
    user_id int,
    foreign key(user_id) references users(user_id)
);

create table if not exists skill_rating (
    skill_rating_id int primary key,
    rating double,
    comment varchar(1024),
    rater_id int,
    foreign key(rater_id) references users(user_id)
);

create table if not exists text_messages (
    message_id int primary key,
    target_user_id int,
    message_text varchar(4096),
    response_text varchar(4096),
    date_sent datetime,
    user_id int,
    foreign key(user_id) references users(user_id),
    foreign key(target_user_id) references users(user_id)
);

create table if not exists job_postings (
    job_id int primary key,
    job_title varchar(255),
    job_type varchar(128),
    job_category varchar(255),
    job_description varchar(1024),
    date_posted datetime,
    image_attachment_url varchar(2000),
    user_id int,
    foreign key(user_id) references users(user_id)
);

create table if not exists media (
    media_id int primary key,
    short_video_id int,
    media_url varchar(2000),
    date_uploaded datetime,
    user_id int,
    foreign key(user_id) references users(user_id)
);

create table if not exists report_contents (
    report_id int primary key,
    report_type varchar(255)
);

