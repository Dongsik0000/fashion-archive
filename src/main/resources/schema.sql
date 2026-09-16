CREATE TABLE users (
  id            BIGSERIAL PRIMARY KEY,
  login_id      VARCHAR(50)  NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role          VARCHAR(20)  NOT NULL DEFAULT 'ADMIN'
);

CREATE TABLE category (
  id         SMALLSERIAL PRIMARY KEY,
  slug       VARCHAR(30) NOT NULL UNIQUE,
  name       VARCHAR(30) NOT NULL,
  sort_order SMALLINT    NOT NULL
);
INSERT INTO category (slug, name, sort_order) VALUES
  ('spring','봄',1), ('summer','여름',2), ('autumn','가을',3), ('winter','겨울',4),
  ('accessory','악세사리',5), ('shoes','신발',6);

CREATE TABLE photo (
  id         BIGSERIAL PRIMARY KEY,
  owner_id   BIGINT       NOT NULL REFERENCES users(id),
  title      VARCHAR(100) NOT NULL,
  memo       TEXT,
  image_url  TEXT         NOT NULL,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE photo_category (
  photo_id    BIGINT   NOT NULL REFERENCES photo(id) ON DELETE CASCADE,
  category_id SMALLINT NOT NULL REFERENCES category(id),
  PRIMARY KEY (photo_id, category_id)
);
CREATE INDEX ON photo_category (category_id);

CREATE TABLE product_link (
  id         BIGSERIAL PRIMARY KEY,
  photo_id   BIGINT      NOT NULL REFERENCES photo(id) ON DELETE CASCADE,
  item_label VARCHAR(30) NOT NULL,
  url        TEXT        NOT NULL,
  title      VARCHAR(100),
  sort_order INT         NOT NULL DEFAULT 0
);
CREATE INDEX ON product_link (photo_id, item_label, sort_order);
