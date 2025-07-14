create database if not exists trademart;

use trademart;

create table if not exists users (
    user_id int primary key,
    username varchar(35) unique,
    name varchar(100),
    email varchar(254),
    password varchar(128),
    password_salt varchar(24),
    prefered_posts varchar(128),
    profile_picture_path varchar(2048),
    verified bit
);

create table if not exists posts (
    post_id int primary key,
    title varchar(128),
    description varchar(2048),
    attached_media varchar(4098),
    likes int,
    user_id int,
    foreign key(user_id) references users(user_id) on delete cascade
);

create table if not exists skill_cards (
    skill_card_id int primary key,
    skill_title varchar(256),
    skill_description varchar(1024),
    tags varchar(1024),
    video_url varchar(2048),
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

create table if not exists services (
    service_id int primary key,
    service_title varchar(255),
    service_category varchar(255),
    service_description varchar(1024),
    service_price double,
    service_currency varchar(3),
    date_posted datetime,
    owner_id int,
    client_id int,
    foreign key(owner_id) references users(user_id),
    foreign key(client_id) references users(user_id)
);

create table if not exists job_listings (
    job_id int primary key,
    job_title varchar(255),
    job_type varchar(128),
    job_category varchar(255),
    job_description varchar(1024),
    date_posted datetime,
    owner_id int,
    foreign key(owner_id) references users(user_id)
);

create table if not exists media (
    media_id int primary key,
    media_type varchar(5),
    short_video_id int,
    media_url varchar(2048),
    date_uploaded datetime,
    user_id int,
    foreign key(user_id) references users(user_id)
);

create table if not exists post_media (
    media_id int,
    post_id int,
    foreign key(media_id) references media(media_id) on delete cascade,
    foreign key(post_id) references posts(post_id) on delete cascade
);

create table if not exists report_contents (
    report_id int primary key,
    report_type varchar(255)
);

create table if not exists followers (
    followed_user_id int,
    follower_user_id int,
    foreign key(followed_user_id) references users(user_id),
    foreign key(follower_user_id) references users(user_id) on delete cascade
);

