-- Операционная дата с биржи (TESYSTIME)

CREATE OR REPLACE FUNCTION tr_get_const.set_oper_date_time(p_micex_time text)
 RETURNS void
 LANGUAGE plpgsql
AS $$
BEGIN
    -- Заглушка для диплома: при появлении tr_oper_date / tr_rule — замените на UPDATE/вызов правила.
    PERFORM 1;
END;
$$;
