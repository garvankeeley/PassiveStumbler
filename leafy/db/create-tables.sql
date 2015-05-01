CREATE TABLE GRIDCELL (
    gridcell_pk serial NOT NULL,
    CONSTRAINT GRIDCELL_pk PRIMARY KEY(gridcell_pk)
);
SELECT AddGeometryColumni('gridcell', 'wkb_geometry', 4326, 'POLYGON', 2);

CREATE TABLE USERINFO (
    userinfo_pk bigserial  NOT NULL,
    name varchar(200)  NOT NULL,
    CONSTRAINT USERINFO_pk PRIMARY KEY (userinfo_pk)
);
ALTER TABLE userinfo ADD UNIQUE(name);

CREATE TABLE WEEK (
    week_num smallint  NOT NULL,
    year int  NOT NULL,
    observations int,
    discovery_cells int,
    discovery_observation int,
    userinfo_fk bigint  NOT NULL,
    gridcell_fk bigint  NOT NULL,
    CONSTRAINT WEEK_pk PRIMARY KEY (year,userinfo_fk,gridcell_fk)
);
ALTER TABLE WEEK ADD CONSTRAINT WEEK_GRIDCELL
    FOREIGN KEY (gridcell_fk)
    REFERENCES GRIDCELL (gridcell_pk)
    NOT DEFERRABLE
    INITIALLY IMMEDIATE
;
ALTER TABLE WEEK ADD CONSTRAINT WEEK_USERINFO
    FOREIGN KEY (userinfo_fk)
    REFERENCES USERINFO (userinfo_pk)
    NOT DEFERRABLE
    INITIALLY IMMEDIATE
;

