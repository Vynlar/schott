-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, email, password)
VALUES (:id, :email, :password)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET email = :email
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :id

-- :name get-user-by-email :? :1
-- :doc retrieves a user record given an email
SELECT * FROM users
WHERE email = :email

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id

-- :name get-shots-by-user! :? :*
-- :doc finds all the shots for a particular user id
SELECT * FROM shots
WHERE user_id = :user_id
