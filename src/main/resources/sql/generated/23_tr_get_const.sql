-- Константы загрузчика

CREATE SCHEMA IF NOT EXISTS tr_get_const;
CREATE OR REPLACE FUNCTION tr_get_const.gc_src_micex()
 RETURNS numeric
AS $$
BEGIN
    RETURN 2432;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tr_get_const.gc_deal_place_section_state()
 RETURNS numeric
AS $$
BEGIN
    RETURN 1774;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tr_get_const.gc_deal_place_section_share()
 RETURNS numeric
AS $$
BEGIN
    RETURN 1775;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tr_get_const.gc_mmvb_sect_fx()
 RETURNS numeric
AS $$
BEGIN
    RETURN 6246;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tr_get_const.gc_buff_micex_deal()
 RETURNS numeric
AS $$
BEGIN
    RETURN 2323;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tr_get_const.gc_buff_micex_deal_sec()
 RETURNS numeric
AS $$
BEGIN
    RETURN 2324;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tr_get_const.gc_buff_micex_quote_sec()
 RETURNS numeric
AS $$
BEGIN
    RETURN 2325;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tr_get_const.gc_buff_micex_order_sec()
 RETURNS numeric
AS $$
BEGIN
    RETURN 5885;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tr_get_const.gc_buff_micex_decimal()
 RETURNS numeric
AS $$
BEGIN
    RETURN 5886;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tr_get_const.gc_buff_micex_board()
 RETURNS numeric
AS $$
BEGIN
    RETURN 5887;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tr_get_const.gc_buff_micex_quote_fx()
 RETURNS numeric
AS $$
BEGIN
    RETURN 5922;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tr_get_const.gc_buff_micex_lotsize()
 RETURNS numeric
AS $$
BEGIN
    RETURN 6181;
END;
$$ LANGUAGE plpgsql;