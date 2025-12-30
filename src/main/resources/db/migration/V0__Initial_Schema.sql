--
-- PostgreSQL database dump
--

-- Dumped from database version 16.0 (Debian 16.0-1.pgdg120+1)
-- Dumped by pg_dump version 16.0 (Debian 16.0-1.pgdg120+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: admin; Type: TABLE; Schema: public; Owner: paladin
--

CREATE TABLE public.admin (
    created_at timestamp(6) without time zone NOT NULL,
    last_login timestamp(6) without time zone,
    id uuid NOT NULL,
    password_hash character varying(255) NOT NULL,
    username character varying(255) NOT NULL
);


ALTER TABLE public.admin OWNER TO paladin;

--
-- Name: cover_letter_templates; Type: TABLE; Schema: public; Owner: paladin
--

CREATE TABLE public.cover_letter_templates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    style_category character varying(50) NOT NULL,
    template_text text NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.cover_letter_templates OWNER TO paladin;

--
-- Name: cv; Type: TABLE; Schema: public; Owner: paladin
--

CREATE TABLE public.cv (
    size bigint NOT NULL,
    uploaded_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    content_type character varying(255) NOT NULL,
    file_name character varying(255) NOT NULL,
    url character varying(255) NOT NULL
);


ALTER TABLE public.cv OWNER TO paladin;

--
-- Name: feature_request; Type: TABLE; Schema: public; Owner: paladin
--

CREATE TABLE public.feature_request (
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    category character varying(50) NOT NULL,
    status character varying(50) NOT NULL,
    title character varying(200) NOT NULL,
    admin_response text,
    description text NOT NULL,
    CONSTRAINT feature_request_category_check CHECK (((category)::text = ANY ((ARRAY['AI_FEATURES'::character varying, 'EMAIL_AUTOMATION'::character varying, 'UI_UX'::character varying, 'JOB_TRACKING'::character varying, 'CV_MANAGEMENT'::character varying, 'INTEGRATIONS'::character varying, 'ANALYTICS'::character varying, 'PERFORMANCE'::character varying, 'MOBILE'::character varying, 'OTHER'::character varying])::text[]))),
    CONSTRAINT feature_request_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'UNDER_REVIEW'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'REJECTED'::character varying])::text[])))
);


ALTER TABLE public.feature_request OWNER TO paladin;

--
-- Name: feature_request_vote; Type: TABLE; Schema: public; Owner: paladin
--

CREATE TABLE public.feature_request_vote (
    created_at timestamp(6) without time zone NOT NULL,
    feature_request_id uuid NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.feature_request_vote OWNER TO paladin;

--
-- Name: job_application; Type: TABLE; Schema: public; Owner: paladin
--

CREATE TABLE public.job_application (
    sent_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    profile_id uuid NOT NULL,
    company character varying(255) NOT NULL,
    job_email character varying(255) NOT NULL,
    job_title character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT job_application_status_check CHECK (((status)::text = ANY ((ARRAY['SENT'::character varying, 'INTERVIEW'::character varying, 'REJECTED'::character varying, 'ACCEPTED'::character varying, 'FOLLOW_UP'::character varying])::text[])))
);


ALTER TABLE public.job_application OWNER TO paladin;

--
-- Name: notification; Type: TABLE; Schema: public; Owner: paladin
--

CREATE TABLE public.notification (
    is_read boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    related_entity_id uuid,
    user_id uuid NOT NULL,
    related_entity_type character varying(50),
    type character varying(50) NOT NULL,
    title character varying(200) NOT NULL,
    message text NOT NULL,
    CONSTRAINT notification_type_check CHECK (((type)::text = ANY ((ARRAY['STATUS_UPDATE'::character varying, 'ADMIN_RESPONSE'::character varying, 'SUBSCRIBED_UPDATE'::character varying, 'FEATURE_ANNOUNCEMENT'::character varying, 'SYSTEM_ANNOUNCEMENT'::character varying])::text[])))
);


ALTER TABLE public.notification OWNER TO paladin;

--
-- Name: profile; Type: TABLE; Schema: public; Owner: paladin
--

CREATE TABLE public.profile (
    created_at timestamp(6) without time zone NOT NULL,
    cv_id uuid,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    summary character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    skills text[]
);


ALTER TABLE public.profile OWNER TO paladin;

--
-- Name: users; Type: TABLE; Schema: public; Owner: paladin
--

CREATE TABLE public.users (
    access_token_expiry timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    id uuid NOT NULL,
    access_token character varying(2048),
    refresh_token character varying(2048),
    auth_provider character varying(255),
    email character varying(255) NOT NULL,
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL,
    CONSTRAINT users_auth_provider_check CHECK (((auth_provider)::text = 'GOOGLE'::text))
);


ALTER TABLE public.users OWNER TO paladin;

--
-- Name: admin admin_pkey; Type: CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.admin
    ADD CONSTRAINT admin_pkey PRIMARY KEY (id);


--
-- Name: admin admin_username_key; Type: CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.admin
    ADD CONSTRAINT admin_username_key UNIQUE (username);


--
-- Name: cover_letter_templates cover_letter_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.cover_letter_templates
    ADD CONSTRAINT cover_letter_templates_pkey PRIMARY KEY (id);


--
-- Name: cv cv_pkey; Type: CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.cv
    ADD CONSTRAINT cv_pkey PRIMARY KEY (id);


--
-- Name: feature_request feature_request_pkey; Type: CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.feature_request
    ADD CONSTRAINT feature_request_pkey PRIMARY KEY (id);


--
-- Name: feature_request_vote feature_request_vote_feature_request_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.feature_request_vote
    ADD CONSTRAINT feature_request_vote_feature_request_id_user_id_key UNIQUE (feature_request_id, user_id);


--
-- Name: feature_request_vote feature_request_vote_pkey; Type: CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.feature_request_vote
    ADD CONSTRAINT feature_request_vote_pkey PRIMARY KEY (id);


--
-- Name: job_application job_application_pkey; Type: CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.job_application
    ADD CONSTRAINT job_application_pkey PRIMARY KEY (id);


--
-- Name: notification notification_pkey; Type: CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);


--
-- Name: profile profile_cv_id_key; Type: CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT profile_cv_id_key UNIQUE (cv_id);


--
-- Name: profile profile_pkey; Type: CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT profile_pkey PRIMARY KEY (id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

--
-- Name: job_application fk9mul7mejds42wvbgu62clpf0y; Type: FK CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.job_application
    ADD CONSTRAINT fk9mul7mejds42wvbgu62clpf0y FOREIGN KEY (profile_id) REFERENCES public.profile(id);


--
-- Name: notification fk_notification_user; Type: FK CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.notification
    ADD CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: feature_request_vote fkbuodjsnjxb8ee9dlt2clcp38y; Type: FK CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.feature_request_vote
    ADD CONSTRAINT fkbuodjsnjxb8ee9dlt2clcp38y FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: profile fkek4fk4j528tnq0iu9kuadc7lh; Type: FK CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT fkek4fk4j528tnq0iu9kuadc7lh FOREIGN KEY (cv_id) REFERENCES public.cv(id);


--
-- Name: feature_request_vote fkgvuihd1f0dn7fawb0j1egplph; Type: FK CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.feature_request_vote
    ADD CONSTRAINT fkgvuihd1f0dn7fawb0j1egplph FOREIGN KEY (feature_request_id) REFERENCES public.feature_request(id);


--
-- Name: feature_request fkql12i5kvo9spelyue4sg0cbfq; Type: FK CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.feature_request
    ADD CONSTRAINT fkql12i5kvo9spelyue4sg0cbfq FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: profile fks14jvsf9tqrcnly0afsv0ngwv; Type: FK CONSTRAINT; Schema: public; Owner: paladin
--

ALTER TABLE ONLY public.profile
    ADD CONSTRAINT fks14jvsf9tqrcnly0afsv0ngwv FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- PostgreSQL database dump complete
--

