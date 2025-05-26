create database if not exists trademart;

use trademart;

create table if not exists user (
    user_id int primary key auto_increment,
    name varchar(256),
    email varchar(256),
    password varchar(256),
    prefered_posts varchar(128),
    verified bit
);

create table if not exists skill_card (
    skill_card_id int primary key auto_increment,
    skill_title varchar(256),
    skill_description varchar(1024),
    tags varchar(1024),
    video_url varchar(2000),
    user_id int,
    foreign key(user_id) references user(user_id)
);

create table if not exists text_messages (
    message_id int primary key auto_increment,
    target_user_id int,
    message_text varchar(4096),
    response_text varchar(4096),
    date_sent datetime,
    user_id int,
    foreign key(user_id) references user(user_id),
    foreign key(target_user_id) references user(target_user_id)
);

create table if not exists job_posting (
    job_id int primary key auto_increment,
    job_title varchar(255),
    job_type varchar(128),
    job_category varchar(255),
    job_description varchar(1024),
    date_posted datetime,
    image_attachment_url varchar(2000),
    user_id int,
    foreign key(user_id) references user(user_id)
);

create table if not exists media (
    media_id int primary key auto_increment,
    short_video_id int,
    media_url varchar(2000),
    date_uploaded datetime,
    user_id int,
    foreign key(user_id) references user(user_id)
);

create table if not exists report_content (
    report_id int primary key auto_increment,
    report_type varchar(255),
);

