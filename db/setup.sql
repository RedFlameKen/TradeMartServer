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
    post_category varchar(255),
    date_posted datetime,
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
    likes int default 0,
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
    amount double,
    likes int default 0,
    job_category varchar(255),
    job_description varchar(1024),
    date_posted datetime,
    employer_id int,
    foreign key(employer_id) references users(user_id)
);

create table if not exists job_transactions (
    id int primary key,
    job_id int,
    employee_id int,
    employer_id int,
    date_published datetime,
    date_started datetime,
    date_finished datetime,
    completed boolean,
    foreign key(job_id) references job_listings(job_id),
    foreign key(employee_id) references users(user_id),
    foreign key(employer_id) references users(user_id)
);

create table if not exists post_categories (
    post_id int,
    category varchar(255),
    foreign key(post_id) references posts(post_id)
);

create table if not exists service_categories (
    service_id int,
    category varchar(255),
    foreign key(service_id) references services(service_id)
);

create table if not exists job_categories (
    job_id int,
    category varchar(255),
    foreign key(job_id) references job_listings(job_id)
);

create table if not exists post_likes (
    user_id int,
    post_id int,
    foreign key(user_id) references users(user_id),
    foreign key(post_id) references posts(post_id)
);

create table if not exists service_likes (
    user_id int,
    service_id int,
    foreign key(user_id) references users(user_id),
    foreign key(service_id) references services(service_id)
);

create table if not exists job_likes (
    user_id int,
    job_id int,
    foreign key(user_id) references users(user_id),
    foreign key(job_id) references job_listings(job_id)
);

create table if not exists user_preferences (
    user_id int,
    preferred_category varchar(255),
    foreign key(user_id) references users(user_id)
);

create table if not exists payments (
    payment_id int primary key,
    type varchar(7),
    amount double,
    is_confirmed tinyint(1),
    receiver_id int,
    sender_id int,
    foreign key(receiver_id) references users(user_id),
    foreign key(sender_id) references users(user_id)
);

create table if not exists job_payment (
    payment_id int,
    job_id int,
    foreign key(payment_id) references payments(payment_id),
    foreign key(job_id) references job_listings(job_id)
);

create table if not exists service_payment (
    payment_id int,
    service_id int,
    foreign key(payment_id) references payments(payment_id),
    foreign key(service_id) references services(service_id)
);

create table if not exists convos (
    convo_id int primary key,
    user1_id int,
    user2_id int,
    foreign key(user1_id) references users(user_id),
    foreign key(user2_id) references users(user_id)
);

create table if not exists chats (
    chat_id int primary key,
    convo_type varchar(7),
    time_sent datetime,
    sender_id int,
    convo_id int,
    foreign key(sender_id) references users(user_id),
    foreign key(convo_id) references convos(convo_id)
);

create table if not exists message_chat (
    chat_id int,
    message varchar(4096),
    foreign key(chat_id) references chats(chat_id)
);

create table if not exists media_chat (
    chat_id int,
    media_id int,
    foreign key(media_id) references media(media_id),
    foreign key(chat_id) references chats(chat_id)
);

create table if not exists payment_chat (
    chat_id int,
    payment_id int,
    foreign key(payment_id) references payments(payment_id),
    foreign key(chat_id) references chats(chat_id)
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

create table if not exists job_media (
    media_id int,
    job_id int,
    foreign key(media_id) references media(media_id) on delete cascade,
    foreign key(job_id) references job_listings(job_id) on delete cascade
);

create table if not exists service_media (
    media_id int,
    service_id int,
    foreign key(media_id) references media(media_id) on delete cascade,
    foreign key(service_id) references services(service_id) on delete cascade
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

create table if not exists reports(
    report_id int primary key,
    message varchar(1024),
    type varchar(255),
    user_id int,
    target_id int,
    FOREIGN KEY(user_id) REFERENCES users(user_id)
);

create table if not exists followers (
    followed_user_id int,
    follower_user_id int,
    foreign key(followed_user_id) references users(user_id),
    foreign key(follower_user_id) references users(user_id) on delete cascade
);

