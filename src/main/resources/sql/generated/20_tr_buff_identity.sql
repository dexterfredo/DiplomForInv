-- buff_id: auto-increment for numeric(20) via SEQUENCE (PostgreSQL IDENTITY only supports int types)
-- Apply once on existing DB. INSERT ... RETURNING buff_id works with DEFAULT nextval(...)

DO $$
DECLARE
    max_id numeric;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_sequences
         WHERE schemaname = 'tr__data_temp'
           AND sequencename = 'tr_buff_buff_id_seq'
    ) THEN
        SELECT COALESCE(MAX(buff_id), 0) INTO max_id FROM tr__data_temp.tr_buff;
        EXECUTE format(
            'CREATE SEQUENCE tr__data_temp.tr_buff_buff_id_seq START WITH %s',
            GREATEST(max_id + 1, 1)
        );
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = 'tr__data_temp'
           AND table_name = 'tr_buff'
           AND column_name = 'buff_id'
           AND column_default IS NULL
    ) THEN
        ALTER TABLE tr__data_temp.tr_buff
            ALTER COLUMN buff_id SET DEFAULT nextval('tr__data_temp.tr_buff_buff_id_seq'::regclass);
        ALTER SEQUENCE tr__data_temp.tr_buff_buff_id_seq OWNED BY tr__data_temp.tr_buff.buff_id;
    END IF;
END $$;
