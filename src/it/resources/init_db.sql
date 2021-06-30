--- extensions:
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE type_operation AS ENUM
    ('S', 'I', 'U', 'D');

--- functions:
CREATE OR REPLACE FUNCTION f_check_permissions(
	p_object_name text,
	p_roles_arr text[],
	p_operation type_operation DEFAULT 'S'::type_operation,
	p_columns_arr text[] DEFAULT NULL::text[])
    RETURNS boolean
    LANGUAGE 'plpgsql'
AS '
DECLARE
  l_ret BOOLEAN;
BEGIN
  RETURN TRUE;
END;';

CREATE OR REPLACE FUNCTION f_row_insert(p_table_name text, p_sys_key_val hstore, p_business_key_val hstore, p_roles_arr text[])
RETURNS uuid
LANGUAGE plpgsql
AS '
DECLARE
base_val integer;
BEGIN
 return uuid_generate_v4 ();
END;';

--- procedures
CREATE OR REPLACE PROCEDURE p_row_update(p_table_name text, p_uuid uuid, p_sys_key_val hstore, p_business_key_val hstore, p_roles_arr text[])
    language plpgsql
as '
DECLARE
base_val integer;
BEGIN
END;';

CREATE OR REPLACE PROCEDURE p_row_delete(p_table_name text, p_uuid uuid, p_sys_key_val hstore, p_roles_arr text[])
    language plpgsql
as '
DECLARE
base_val integer;
BEGIN
END;';

--- enum
CREATE TYPE type_gender as ENUM ('W', 'M');

--- file
CREATE TYPE type_file as (
    id text,
    checksum text
);

--- test_entity:
create table test_entity
(
    id      UUID NOT NULL,
    ddm_created_at       timestamp,
    ddm_updated_at       timestamp,
    ddm_created_by       varchar(255),
    ddm_updated_by      varchar(255),
    consent_date     timestamptz,
    person_full_name   varchar(255),
    person_pass_number   varchar(255),
    person_gender       type_gender,
	  CONSTRAINT pk_test_entity PRIMARY KEY (id)
);

insert into test_entity (id, ddm_created_at, ddm_updated_at,
        ddm_created_by, ddm_updated_by,
        consent_date, person_full_name, person_pass_number, person_gender)
values ('3cc262c1-0cd8-4d45-be66-eb0fca821e0a', current_timestamp, current_timestamp,
        current_user, current_user,
        '2020-01-15 12:00:01+02:00', 'John Doe Patronymic', 'AB123456', 'M'),
       ('1ce1cad1-ff11-1fa1-b111-e07afea1cb1d', current_timestamp, current_timestamp,
        current_user, current_user,
        current_timestamp, 'John Doe Patronymic', 'BA654321', 'M'),
       ('9ce4cad9-ff50-4fa3-b893-e07afea0cb8d', current_timestamp, current_timestamp,
        current_user, current_user,
        '2020-01-15 12:00:01+02:00','Benjamin Franklin Patronymic', 'XY098765', 'W');

--- test_entity_file:
CREATE TABLE test_entity_file
(
	id uuid NOT NULL,
	legal_entity_name text NOT NULL,
	scan_copy type_file,
	ddm_created_at timestamptz NOT NULL DEFAULT now(),
	ddm_created_by text NOT NULL,
	ddm_updated_at timestamptz NOT NULL DEFAULT now(),
	ddm_updated_by text NOT NULL,
	CONSTRAINT pk_test_entity_file PRIMARY KEY (id)
);

insert into test_entity_file (id, legal_entity_name, scan_copy,
        ddm_created_at, ddm_updated_at, ddm_created_by, ddm_updated_by)
values ('7f017d37-6ba5-4849-a4b2-f6a3ef2cadb9', 'FOP John Doe',
         '(1,0d5f97dd25b50267a1c03fba4d649d56d3d818704d0dcdfa692db62119b1221a)',
        current_timestamp, current_timestamp, current_user, current_user
        ),
        ('7300a76f-4a9f-457a-8ec5-08d258d2282a', 'FOP Another John Doe',
        '(2,be7b2c0ccc2e309c7aeb78aa3afa05d29f19bb94865cdd65ea831f629013656a)',
        current_timestamp, current_timestamp, current_user, current_user
        );

--- test_entity_file_array:
CREATE TABLE test_entity_file_array
(
    id uuid NOT NULL,
    legal_entity_name text NOT NULL,
    scan_copies _type_file,
    ddm_created_at timestamptz NOT NULL DEFAULT now(),
    ddm_created_by text NOT NULL,
    ddm_updated_at timestamptz NOT NULL DEFAULT now(),
    ddm_updated_by text NOT NULL,
    CONSTRAINT pk_test_entity_file_array PRIMARY KEY (id)
);

insert into test_entity_file_array (id, legal_entity_name, scan_copies,
                              ddm_created_at, ddm_updated_at, ddm_created_by, ddm_updated_by)
values ('7f017d37-6ba5-4849-a4b2-f6a3ef2cadb9', 'FOP John Doe',
        array['(1,db7bb0ef3ae21cafba57068bab4bcdd5129ba8a25ef5f8c16ad33fc686c7467e)',
            '(2,2a3d2db6e3974ee0fae45b0a3f0616c645b8a80b72c153d0577b35cbdfe41dd4)']::type_file[],
        current_timestamp, current_timestamp, current_user, current_user
       ),
       ('7300a76f-aaaa-bbbb-cccc-08d258d2282a', 'FOP Another John Doe',
        array['(3,6be438aa51ef8ff668b12925b301da585cf84d38fed32918d78d430dc133f5ab)',
            '(4,9d87053d47ff9064203b63ee770697872a211fd1b8087267178cbfb5371f4343)']::type_file[],
        current_timestamp, current_timestamp, current_user, current_user
       );


--- test_entity_m2m:
CREATE TABLE test_entity_m2m
(
	id uuid NOT NULL,
	name text NOT NULL,
    entities _uuid NOT NULL,
	ddm_created_at timestamptz NOT NULL DEFAULT now(),
	ddm_created_by text NOT NULL,
	ddm_updated_at timestamptz NOT NULL DEFAULT now(),
	ddm_updated_by text NOT NULL,
	CONSTRAINT pk_test_entity_m2m PRIMARY KEY (id)
);

insert into test_entity_m2m (id, name, entities,
        ddm_created_at, ddm_updated_at, ddm_created_by, ddm_updated_by)
values ('7f017d37-6ba5-4849-a4b2-f6a3ef2cadb9', 'FOP John Doe',
        array['3cc262c1-0cd8-4d45-be66-eb0fca821e0a', '9ce4cad9-ff50-4fa3-b893-e07afea0cb8d']::uuid[],
        current_timestamp, current_timestamp, current_user, current_user
        ),
        ('7300a76f-4a9f-457a-8ec5-08d258d2282a', 'FOP Another John Doe',
        array['3cc262c1-0cd8-4d45-be66-eb0fca821e0a']::uuid[],
        current_timestamp, current_timestamp, current_user, current_user
        );

--- views:
CREATE OR REPLACE VIEW test_entity_by_enum_and_name_starts_with_limit_offset_v AS
SELECT c.id, c.person_full_name, c.person_gender
FROM test_entity c;

CREATE OR REPLACE VIEW test_entity_file_by_legal_entity_name_starts_with_v AS
SELECT c.id, c.legal_entity_name, c.scan_copy
FROM test_entity_file c;