-- :name add-adress! :! :n
-- :doc  adds a new addres
INSERT INTO address
    (id, pass)
    VALUES (:street, :number, :post_code, :city, :notes)