BEGIN;

ALTER TABLE public.users
    DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE public.users
    ADD CONSTRAINT users_role_check
    CHECK (role IN ('USER', 'RECRUITER', 'ADMIN'));

UPDATE public.users
SET role = 'RECRUITER', requested_role = NULL
WHERE role = 'USER' AND requested_role = 'RECRUITER';

COMMIT;
