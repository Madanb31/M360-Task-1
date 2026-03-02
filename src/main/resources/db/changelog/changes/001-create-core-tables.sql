-- liquibase formatted sql

-- changeset madan:1
CREATE TABLE addresses (
                           id UUID NOT NULL,
                           city VARCHAR(255),
                           state VARCHAR(255),
                           street VARCHAR(255),
                           zipcode VARCHAR(255),
                           PRIMARY KEY (id)
);

CREATE TABLE roles (
                       id UUID NOT NULL,
                       role_name VARCHAR(255) NOT NULL UNIQUE,
                       PRIMARY KEY (id)
);

CREATE TABLE users (
                       id UUID NOT NULL,
                       contact_num VARCHAR(255),
                       email VARCHAR(255) NOT NULL UNIQUE,
                       name VARCHAR(255),
                       password VARCHAR(255) NOT NULL,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       address_id UUID,
                       PRIMARY KEY (id),
                       CONSTRAINT fk_users_address FOREIGN KEY (address_id) REFERENCES addresses (id)
);

CREATE TABLE user_roles (
                            user_id UUID NOT NULL,
                            role_id UUID NOT NULL,
                            PRIMARY KEY (user_id, role_id),
                            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
                            CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);