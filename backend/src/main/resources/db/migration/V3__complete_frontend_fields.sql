ALTER TABLE type_institution
    ADD COLUMN description TEXT;

ALTER TABLE institution
    ADD COLUMN address VARCHAR(300),
    ADD COLUMN phone VARCHAR(50),
    ADD COLUMN email VARCHAR(254);

ALTER TABLE location
    ADD COLUMN capacity INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN description TEXT,
    ADD CONSTRAINT chk_location_capacity CHECK (capacity >= 0);
