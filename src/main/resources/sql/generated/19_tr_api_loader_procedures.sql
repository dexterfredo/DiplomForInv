CREATE SCHEMA IF NOT EXISTS tr_api_loader;

-- type_buff=2324 tr_buff_micex_deal_sec
CREATE OR REPLACE PROCEDURE tr_api_loader.micex_deal_sec_data_ins(INOUT ior_buff tr__data_view.v_tr_buff_micex_deal_sec)
LANGUAGE plpgsql AS $$
DECLARE
    r_buff_cat tr__data_temp.tr_buff%ROWTYPE;
    v_buff_id NUMERIC;
BEGIN
    r_buff_cat.type_section := ior_buff.type_section;
    r_buff_cat.type_buff := 2324;
    r_buff_cat.type_src := ior_buff.type_src;
    r_buff_cat.type_format := ior_buff.type_format;
    r_buff_cat.insert_datetime := NOW();
    INSERT INTO tr__data_temp.tr_buff (type_section, type_buff, type_format, type_src, insert_datetime)
    VALUES (r_buff_cat.type_section, r_buff_cat.type_buff, r_buff_cat.type_format, r_buff_cat.type_src, r_buff_cat.insert_datetime)
    RETURNING buff_id INTO v_buff_id;
    ior_buff.buff_id := v_buff_id;
    INSERT INTO tr__data_temp.tr_buff_micex_deal_sec (id, tradeno, orderno, tradedatetime, buysell, userid, firmid, account_, secboard, seccode, price, quantity, value_, settledate, accruedint, yield, period, settlecode, tradetype, commission, reporate, repoterm, startdiscount, lowerdiscount, upperdiscount, clearingcentercomm, exchangecomm, tradingsystemcomm, raw_tradeno, raw_orderno, raw_tradetime, raw_buysell, raw_userid, raw_firmid, raw_account_, raw_secboard, raw_seccode, raw_price, raw_quantity, raw_value_, raw_settledate, raw_accruedint, raw_yield, raw_period, raw_settlecode, raw_tradetype, raw_commission, raw_reporate, raw_repoterm, raw_startdiscount, raw_lowerdiscount, raw_upperdiscount, raw_clearingcentercomm, raw_exchangecomm, raw_tradingsystemcomm, brokerref, price2, raw_brokerref, raw_price2, raw_tradedate, clientcode, raw_clientcode, accrued2, raw_accrued2, cpfirmid, raw_cpfirmid, repovalue, raw_repovalue, repo2value, raw_repo2value, blocksecurities, raw_blocksecurities, text, parenttradeno, lotsize, decimals, price_ccy, clear_ccy, raw_price_ccy, raw_clear_ccy, ref_buff_id)
    VALUES (v_buff_id, COALESCE(NULLIF(TRIM(ior_buff.tradeno), ''), NULLIF(TRIM(ior_buff.raw_tradeno), '')), COALESCE(NULLIF(TRIM(ior_buff.orderno), ''), NULLIF(TRIM(ior_buff.raw_orderno), '')), ior_buff.tradedatetime, COALESCE(NULLIF(TRIM(ior_buff.buysell), ''), NULLIF(TRIM(ior_buff.raw_buysell), '')), COALESCE(NULLIF(TRIM(ior_buff.userid), ''), NULLIF(TRIM(ior_buff.raw_userid), '')), COALESCE(NULLIF(TRIM(ior_buff.firmid), ''), NULLIF(TRIM(ior_buff.raw_firmid), '')), COALESCE(NULLIF(TRIM(ior_buff.account_), ''), NULLIF(TRIM(ior_buff.raw_account_), '')), COALESCE(NULLIF(TRIM(ior_buff.secboard), ''), NULLIF(TRIM(ior_buff.raw_secboard), '')), COALESCE(NULLIF(TRIM(ior_buff.seccode), ''), NULLIF(TRIM(ior_buff.raw_seccode), '')), COALESCE(NULLIF(TRIM(ior_buff.price), ''), NULLIF(TRIM(ior_buff.raw_price), '')), COALESCE(NULLIF(TRIM(ior_buff.quantity), ''), NULLIF(TRIM(ior_buff.raw_quantity), '')), COALESCE(NULLIF(TRIM(ior_buff.value_), ''), NULLIF(TRIM(ior_buff.raw_value_), '')), COALESCE(NULLIF(TRIM(ior_buff.settledate), ''), NULLIF(TRIM(ior_buff.raw_settledate), '')), COALESCE(NULLIF(TRIM(ior_buff.accruedint), ''), NULLIF(TRIM(ior_buff.raw_accruedint), '')), COALESCE(NULLIF(TRIM(ior_buff.yield), ''), NULLIF(TRIM(ior_buff.raw_yield), '')), COALESCE(NULLIF(TRIM(ior_buff.period), ''), NULLIF(TRIM(ior_buff.raw_period), '')), COALESCE(NULLIF(TRIM(ior_buff.settlecode), ''), NULLIF(TRIM(ior_buff.raw_settlecode), '')), COALESCE(NULLIF(TRIM(ior_buff.tradetype), ''), NULLIF(TRIM(ior_buff.raw_tradetype), '')), COALESCE(NULLIF(TRIM(ior_buff.commission), ''), NULLIF(TRIM(ior_buff.raw_commission), '')), COALESCE(NULLIF(TRIM(ior_buff.reporate), ''), NULLIF(TRIM(ior_buff.raw_reporate), '')), COALESCE(NULLIF(TRIM(ior_buff.repoterm), ''), NULLIF(TRIM(ior_buff.raw_repoterm), '')), COALESCE(NULLIF(TRIM(ior_buff.startdiscount), ''), NULLIF(TRIM(ior_buff.raw_startdiscount), '')), COALESCE(NULLIF(TRIM(ior_buff.lowerdiscount), ''), NULLIF(TRIM(ior_buff.raw_lowerdiscount), '')), COALESCE(NULLIF(TRIM(ior_buff.upperdiscount), ''), NULLIF(TRIM(ior_buff.raw_upperdiscount), '')), COALESCE(NULLIF(TRIM(ior_buff.clearingcentercomm), ''), NULLIF(TRIM(ior_buff.raw_clearingcentercomm), '')), COALESCE(NULLIF(TRIM(ior_buff.exchangecomm), ''), NULLIF(TRIM(ior_buff.raw_exchangecomm), '')), COALESCE(NULLIF(TRIM(ior_buff.tradingsystemcomm), ''), NULLIF(TRIM(ior_buff.raw_tradingsystemcomm), '')), ior_buff.raw_tradeno, ior_buff.raw_orderno, ior_buff.raw_tradetime, ior_buff.raw_buysell, ior_buff.raw_userid, ior_buff.raw_firmid, ior_buff.raw_account_, ior_buff.raw_secboard, ior_buff.raw_seccode, ior_buff.raw_price, ior_buff.raw_quantity, ior_buff.raw_value_, ior_buff.raw_settledate, ior_buff.raw_accruedint, ior_buff.raw_yield, ior_buff.raw_period, ior_buff.raw_settlecode, ior_buff.raw_tradetype, ior_buff.raw_commission, ior_buff.raw_reporate, ior_buff.raw_repoterm, ior_buff.raw_startdiscount, ior_buff.raw_lowerdiscount, ior_buff.raw_upperdiscount, ior_buff.raw_clearingcentercomm, ior_buff.raw_exchangecomm, ior_buff.raw_tradingsystemcomm, COALESCE(NULLIF(TRIM(ior_buff.brokerref), ''), NULLIF(TRIM(ior_buff.raw_brokerref), '')), COALESCE(NULLIF(TRIM(ior_buff.price2), ''), NULLIF(TRIM(ior_buff.raw_price2), '')), ior_buff.raw_brokerref, ior_buff.raw_price2, ior_buff.raw_tradedate, COALESCE(NULLIF(TRIM(ior_buff.clientcode), ''), NULLIF(TRIM(ior_buff.raw_clientcode), '')), ior_buff.raw_clientcode, COALESCE(NULLIF(TRIM(ior_buff.accrued2), ''), NULLIF(TRIM(ior_buff.raw_accrued2), '')), ior_buff.raw_accrued2, COALESCE(NULLIF(TRIM(ior_buff.cpfirmid), ''), NULLIF(TRIM(ior_buff.raw_cpfirmid), '')), ior_buff.raw_cpfirmid, COALESCE(NULLIF(TRIM(ior_buff.repovalue), ''), NULLIF(TRIM(ior_buff.raw_repovalue), '')), ior_buff.raw_repovalue, COALESCE(NULLIF(TRIM(ior_buff.repo2value), ''), NULLIF(TRIM(ior_buff.raw_repo2value), '')), ior_buff.raw_repo2value, COALESCE(NULLIF(TRIM(ior_buff.blocksecurities), ''), NULLIF(TRIM(ior_buff.raw_blocksecurities), '')), ior_buff.raw_blocksecurities, COALESCE(ior_buff.text, ''), ior_buff.parenttradeno, NULLIF(TRIM(ior_buff.lotsize::text), '')::numeric, NULLIF(TRIM(ior_buff.decimals::text), '')::numeric, COALESCE(NULLIF(TRIM(ior_buff.price_ccy), ''), NULLIF(TRIM(ior_buff.raw_price_ccy), '')), COALESCE(NULLIF(TRIM(ior_buff.clear_ccy), ''), NULLIF(TRIM(ior_buff.raw_clear_ccy), '')), ior_buff.raw_price_ccy, ior_buff.raw_clear_ccy, ior_buff.ref_buff_id);
END;
$$;

-- type_buff=2325 tr_buff_micex_sec_quote
CREATE OR REPLACE PROCEDURE tr_api_loader.micex_sec_quote_data_ins(INOUT ior_buff tr__data_view.v_tr_buff_micex_sec_quote)
LANGUAGE plpgsql AS $$
DECLARE
    r_buff_cat tr__data_temp.tr_buff%ROWTYPE;
    v_buff_id NUMERIC;
BEGIN
    r_buff_cat.type_section := ior_buff.type_section;
    r_buff_cat.type_buff := 2325;
    r_buff_cat.type_src := ior_buff.type_src;
    r_buff_cat.type_format := ior_buff.type_format;
    r_buff_cat.insert_datetime := NOW();
    INSERT INTO tr__data_temp.tr_buff (type_section, type_buff, type_format, type_src, insert_datetime)
    VALUES (r_buff_cat.type_section, r_buff_cat.type_buff, r_buff_cat.type_format, r_buff_cat.type_src, r_buff_cat.insert_datetime)
    RETURNING buff_id INTO v_buff_id;
    ior_buff.buff_id := v_buff_id;
    INSERT INTO tr__data_temp.tr_buff_micex_sec_quote (id, tradedate, secboard, seccode, faceunit, accruedint, open_, high, low, last_, voltoday, valtoday, waprice, numtrades, marketpricetoday, raw_secboard, raw_seccode, raw_faceunit, raw_decimals, raw_accruedint, raw_open_, raw_high, raw_low, raw_last_, raw_voltoday, raw_valtoday, raw_waprice, raw_numtrades, raw_marketpricetoday, raw_tradedate, text)
    VALUES (v_buff_id, COALESCE(NULLIF(TRIM(ior_buff.tradedate), ''), NULLIF(TRIM(ior_buff.raw_tradedate), '')), COALESCE(NULLIF(TRIM(ior_buff.secboard), ''), NULLIF(TRIM(ior_buff.raw_secboard), '')), COALESCE(NULLIF(TRIM(ior_buff.seccode), ''), NULLIF(TRIM(ior_buff.raw_seccode), '')), COALESCE(NULLIF(TRIM(ior_buff.faceunit), ''), NULLIF(TRIM(ior_buff.raw_faceunit), '')), COALESCE(NULLIF(TRIM(ior_buff.accruedint), ''), NULLIF(TRIM(ior_buff.raw_accruedint), '')), COALESCE(NULLIF(TRIM(ior_buff.open_), ''), NULLIF(TRIM(ior_buff.raw_open_), '')), COALESCE(NULLIF(TRIM(ior_buff.high), ''), NULLIF(TRIM(ior_buff.raw_high), '')), COALESCE(NULLIF(TRIM(ior_buff.low), ''), NULLIF(TRIM(ior_buff.raw_low), '')), COALESCE(NULLIF(TRIM(ior_buff.last_), ''), NULLIF(TRIM(ior_buff.raw_last_), '')), COALESCE(NULLIF(TRIM(ior_buff.voltoday), ''), NULLIF(TRIM(ior_buff.raw_voltoday), '')), COALESCE(NULLIF(TRIM(ior_buff.valtoday), ''), NULLIF(TRIM(ior_buff.raw_valtoday), '')), COALESCE(NULLIF(TRIM(ior_buff.waprice), ''), NULLIF(TRIM(ior_buff.raw_waprice), '')), COALESCE(NULLIF(TRIM(ior_buff.numtrades), ''), NULLIF(TRIM(ior_buff.raw_numtrades), '')), COALESCE(NULLIF(TRIM(ior_buff.marketpricetoday), ''), NULLIF(TRIM(ior_buff.raw_marketpricetoday), '')), ior_buff.raw_secboard, ior_buff.raw_seccode, ior_buff.raw_faceunit, ior_buff.raw_decimals, ior_buff.raw_accruedint, ior_buff.raw_open_, ior_buff.raw_high, ior_buff.raw_low, ior_buff.raw_last_, ior_buff.raw_voltoday, ior_buff.raw_valtoday, ior_buff.raw_waprice, ior_buff.raw_numtrades, ior_buff.raw_marketpricetoday, ior_buff.raw_tradedate, COALESCE(ior_buff.text, ''));
END;
$$;

-- type_buff=5885 tr_buff_micex_sec_oda
CREATE OR REPLACE PROCEDURE tr_api_loader.micex_sec_oda_data_ins(INOUT ior_buff tr__data_view.v_tr_buff_micex_sec_oda)
LANGUAGE plpgsql AS $$
DECLARE
    r_buff_cat tr__data_temp.tr_buff%ROWTYPE;
    v_buff_id NUMERIC;
BEGIN
    r_buff_cat.type_section := ior_buff.type_section;
    r_buff_cat.type_buff := 5885;
    r_buff_cat.type_src := ior_buff.type_src;
    r_buff_cat.type_format := ior_buff.type_format;
    r_buff_cat.insert_datetime := NOW();
    INSERT INTO tr__data_temp.tr_buff (type_section, type_buff, type_format, type_src, insert_datetime)
    VALUES (r_buff_cat.type_section, r_buff_cat.type_buff, r_buff_cat.type_format, r_buff_cat.type_src, r_buff_cat.insert_datetime)
    RETURNING buff_id INTO v_buff_id;
    ior_buff.buff_id := v_buff_id;
    INSERT INTO tr__data_temp.tr_buff_micex_sec_oda (id, orderno, orderdate, status, mktlimit, buysell, splitflag, immcancel, brokerref, userid, firmid, account_, secboard, seccode, price, quantity, balance, value_, accruedint, entrytype, yield, period, price2, extref, settlecode, primarydist, clientcode, withdrawtime, marketmaker, activationtime, raw_orderno, raw_ordertime, raw_status, raw_mktlimit, raw_buysell, raw_splitflag, raw_immcancel, raw_brokerref, raw_userid, raw_firmid, raw_account_, raw_secboard, raw_seccode, raw_price, raw_quantity, raw_balance, raw_value_, raw_accruedint, raw_entrytype, raw_yield, raw_period, raw_price2, raw_extref, raw_settlecode, raw_primarydist, raw_clientcode, raw_withdrawtime, raw_marketmaker, raw_activationtime, raw_orderdate, text, lotsize, decimals, raw_hidden, hidden_)
    VALUES (v_buff_id, COALESCE(NULLIF(TRIM(ior_buff.orderno), ''), NULLIF(TRIM(ior_buff.raw_orderno), '')), COALESCE(NULLIF(TRIM(ior_buff.orderdate), ''), NULLIF(TRIM(ior_buff.raw_orderdate), '')), COALESCE(NULLIF(TRIM(ior_buff.status), ''), NULLIF(TRIM(ior_buff.raw_status), '')), COALESCE(NULLIF(TRIM(ior_buff.mktlimit), ''), NULLIF(TRIM(ior_buff.raw_mktlimit), '')), COALESCE(NULLIF(TRIM(ior_buff.buysell), ''), NULLIF(TRIM(ior_buff.raw_buysell), '')), COALESCE(NULLIF(TRIM(ior_buff.splitflag), ''), NULLIF(TRIM(ior_buff.raw_splitflag), '')), COALESCE(NULLIF(TRIM(ior_buff.immcancel), ''), NULLIF(TRIM(ior_buff.raw_immcancel), '')), COALESCE(NULLIF(TRIM(ior_buff.brokerref), ''), NULLIF(TRIM(ior_buff.raw_brokerref), '')), COALESCE(NULLIF(TRIM(ior_buff.userid), ''), NULLIF(TRIM(ior_buff.raw_userid), '')), COALESCE(NULLIF(TRIM(ior_buff.firmid), ''), NULLIF(TRIM(ior_buff.raw_firmid), '')), COALESCE(NULLIF(TRIM(ior_buff.account_), ''), NULLIF(TRIM(ior_buff.raw_account_), '')), COALESCE(NULLIF(TRIM(ior_buff.secboard), ''), NULLIF(TRIM(ior_buff.raw_secboard), '')), COALESCE(NULLIF(TRIM(ior_buff.seccode), ''), NULLIF(TRIM(ior_buff.raw_seccode), '')), COALESCE(NULLIF(TRIM(ior_buff.price), ''), NULLIF(TRIM(ior_buff.raw_price), '')), COALESCE(NULLIF(TRIM(ior_buff.quantity), ''), NULLIF(TRIM(ior_buff.raw_quantity), '')), COALESCE(NULLIF(TRIM(ior_buff.balance), ''), NULLIF(TRIM(ior_buff.raw_balance), '')), COALESCE(NULLIF(TRIM(ior_buff.value_), ''), NULLIF(TRIM(ior_buff.raw_value_), '')), COALESCE(NULLIF(TRIM(ior_buff.accruedint), ''), NULLIF(TRIM(ior_buff.raw_accruedint), '')), COALESCE(NULLIF(TRIM(ior_buff.entrytype), ''), NULLIF(TRIM(ior_buff.raw_entrytype), '')), COALESCE(NULLIF(TRIM(ior_buff.yield), ''), NULLIF(TRIM(ior_buff.raw_yield), '')), COALESCE(NULLIF(TRIM(ior_buff.period), ''), NULLIF(TRIM(ior_buff.raw_period), '')), COALESCE(NULLIF(TRIM(ior_buff.price2), ''), NULLIF(TRIM(ior_buff.raw_price2), '')), COALESCE(NULLIF(TRIM(ior_buff.extref), ''), NULLIF(TRIM(ior_buff.raw_extref), '')), COALESCE(NULLIF(TRIM(ior_buff.settlecode), ''), NULLIF(TRIM(ior_buff.raw_settlecode), '')), NULLIF(TRIM(ior_buff.raw_primarydist), ''), COALESCE(NULLIF(TRIM(ior_buff.clientcode), ''), NULLIF(TRIM(ior_buff.raw_clientcode), '')), COALESCE(NULLIF(TRIM(ior_buff.withdrawtime), ''), NULLIF(TRIM(ior_buff.raw_withdrawtime), '')), COALESCE(NULLIF(TRIM(ior_buff.marketmaker), ''), NULLIF(TRIM(ior_buff.raw_marketmaker), '')), COALESCE(NULLIF(TRIM(ior_buff.activationtime), ''), NULLIF(TRIM(ior_buff.raw_activationtime), '')), ior_buff.raw_orderno, ior_buff.raw_ordertime, ior_buff.raw_status, ior_buff.raw_mktlimit, ior_buff.raw_buysell, ior_buff.raw_splitflag, ior_buff.raw_immcancel, ior_buff.raw_brokerref, ior_buff.raw_userid, ior_buff.raw_firmid, ior_buff.raw_account_, ior_buff.raw_secboard, ior_buff.raw_seccode, ior_buff.raw_price, ior_buff.raw_quantity, ior_buff.raw_balance, ior_buff.raw_value_, ior_buff.raw_accruedint, ior_buff.raw_entrytype, ior_buff.raw_yield, ior_buff.raw_period, ior_buff.raw_price2, ior_buff.raw_extref, ior_buff.raw_settlecode, ior_buff.raw_primarydist, ior_buff.raw_clientcode, ior_buff.raw_withdrawtime, ior_buff.raw_marketmaker, ior_buff.raw_activationtime, ior_buff.raw_orderdate, COALESCE(ior_buff.text, ''), NULLIF(TRIM(ior_buff.lotsize::text), '')::numeric, NULLIF(TRIM(ior_buff.decimals::text), '')::numeric, ior_buff.raw_hidden, ior_buff.hidden_);
END;
$$;

-- type_buff=5886 tr_buff_micex_decimals
CREATE OR REPLACE PROCEDURE tr_api_loader.micex_decimals_data_ins(INOUT ior_buff tr__data_view.v_tr_buff_micex_decimals)
LANGUAGE plpgsql AS $$
DECLARE
    r_buff_cat tr__data_temp.tr_buff%ROWTYPE;
    v_buff_id NUMERIC;
BEGIN
    r_buff_cat.type_section := ior_buff.type_section;
    r_buff_cat.type_buff := 5886;
    r_buff_cat.type_src := ior_buff.type_src;
    r_buff_cat.type_format := NULL;
    r_buff_cat.insert_datetime := NOW();
    INSERT INTO tr__data_temp.tr_buff (type_section, type_buff, type_format, type_src, insert_datetime)
    VALUES (r_buff_cat.type_section, r_buff_cat.type_buff, r_buff_cat.type_format, r_buff_cat.type_src, r_buff_cat.insert_datetime)
    RETURNING buff_id INTO v_buff_id;
    ior_buff.buff_id := v_buff_id;
    INSERT INTO tr__data_temp.tr_buff_micex_decimals (id, secboard, seccode, decimals, insert_date, text, entry_date, expiry_date, lotsize)
    VALUES (v_buff_id, COALESCE(NULLIF(TRIM(ior_buff.secboard), ''), NULLIF(TRIM(ior_buff.raw_secboard), '')), COALESCE(NULLIF(TRIM(ior_buff.seccode), ''), NULLIF(TRIM(ior_buff.raw_seccode), '')), COALESCE(NULLIF(TRIM(ior_buff.decimals::text), '')::numeric, NULLIF(TRIM(ior_buff.raw_decimals::text), '')::numeric), NOW(), COALESCE(ior_buff.text, ''), NOW(), NULL, COALESCE(NULLIF(TRIM(ior_buff.lotsize::text), '')::numeric, NULLIF(TRIM(ior_buff.raw_lotsize::text), '')::numeric));
END;
$$;

-- type_buff=5887 tr_buff_micex_board
CREATE OR REPLACE PROCEDURE tr_api_loader.micex_board_data_ins(INOUT ior_buff tr__data_view.v_tr_buff_micex_board)
LANGUAGE plpgsql AS $$
DECLARE
    r_buff_cat tr__data_temp.tr_buff%ROWTYPE;
    v_buff_id NUMERIC;
BEGIN
    r_buff_cat.type_section := ior_buff.type_section;
    r_buff_cat.type_buff := 5887;
    r_buff_cat.type_src := ior_buff.type_src;
    r_buff_cat.type_format := ior_buff.type_format;
    r_buff_cat.insert_datetime := NOW();
    INSERT INTO tr__data_temp.tr_buff (type_section, type_buff, type_format, type_src, insert_datetime)
    VALUES (r_buff_cat.type_section, r_buff_cat.type_buff, r_buff_cat.type_format, r_buff_cat.type_src, r_buff_cat.insert_datetime)
    RETURNING buff_id INTO v_buff_id;
    ior_buff.buff_id := v_buff_id;
    INSERT INTO tr__data_temp.tr_buff_micex_board (id, boardid, boardname, status, currcode, marketid, latname, raw_boardid, raw_boardname, raw_status, raw_currcode, raw_marketid, raw_latname, text)
    VALUES (v_buff_id, COALESCE(NULLIF(TRIM(ior_buff.boardid), ''), NULLIF(TRIM(ior_buff.raw_boardid), '')), COALESCE(NULLIF(TRIM(ior_buff.boardname), ''), NULLIF(TRIM(ior_buff.raw_boardname), '')), COALESCE(NULLIF(TRIM(ior_buff.status), ''), NULLIF(TRIM(ior_buff.raw_status), '')), COALESCE(NULLIF(TRIM(ior_buff.currcode), ''), NULLIF(TRIM(ior_buff.raw_currcode), '')), COALESCE(NULLIF(TRIM(ior_buff.marketid), ''), NULLIF(TRIM(ior_buff.raw_marketid), '')), COALESCE(NULLIF(TRIM(ior_buff.latname), ''), NULLIF(TRIM(ior_buff.raw_latname), '')), ior_buff.raw_boardid, ior_buff.raw_boardname, ior_buff.raw_status, ior_buff.raw_currcode, ior_buff.raw_marketid, ior_buff.raw_latname, COALESCE(ior_buff.text, ''));
END;
$$;

-- type_buff=5922 tr_buff_micex_fx_quote
CREATE OR REPLACE PROCEDURE tr_api_loader.micex_fx_quote_data_ins(INOUT ior_buff tr__data_view.v_tr_buff_micex_fx_quote)
LANGUAGE plpgsql AS $$
DECLARE
    r_buff_cat tr__data_temp.tr_buff%ROWTYPE;
    v_buff_id NUMERIC;
BEGIN
    r_buff_cat.type_section := ior_buff.type_section;
    r_buff_cat.type_buff := 5922;
    r_buff_cat.type_src := ior_buff.type_src;
    r_buff_cat.type_format := ior_buff.type_format;
    r_buff_cat.insert_datetime := NOW();
    INSERT INTO tr__data_temp.tr_buff (type_section, type_buff, type_format, type_src, insert_datetime)
    VALUES (r_buff_cat.type_section, r_buff_cat.type_buff, r_buff_cat.type_format, r_buff_cat.type_src, r_buff_cat.insert_datetime)
    RETURNING buff_id INTO v_buff_id;
    ior_buff.buff_id := v_buff_id;
    INSERT INTO tr__data_temp.tr_buff_micex_fx_quote (id, tradedate, secboard, seccode, lotsize, minstep, facevalue, faceunit, prevprice, decimals, prevwaprice, open_, high, low, last_, change, voltoday, valtoday, value_, waprice, numtrades, closeprice, baseprice, raw_tradedate, raw_secboard, raw_seccode, raw_lotsize, raw_minstep, raw_facevalue, raw_faceunit, raw_prevprice, raw_decimals, raw_prevwaprice, raw_open_, raw_high, raw_low, raw_last_, raw_change, raw_voltoday, raw_valtoday, raw_value, raw_waprice, raw_numtrades, raw_closeprice, raw_baseprice, text, raw_secname, secname, bid, raw_bid, biddepth, raw_biddepth, numbids, raw_numbids, offer, raw_offer, offerdepth, raw_offerdepth, offerdeptht, raw_offerdeptht, numoffers, raw_numoffers, qty, raw_qty, highbid, raw_highbid, lowoffer, raw_lowoffer, priceminusprevwaprice, raw_priceminusprevwaprice, numnegdeals, raw_numnegdeals, raw_time, marketcode, raw_marketcode, tradingstatus, raw_tradingstatus, status, raw_status, remarks, raw_remarks, instrid, raw_instrid, biddeptht, raw_biddeptht, marketpricetoday, raw_marketpricetoday)
    VALUES (v_buff_id, COALESCE(NULLIF(TRIM(ior_buff.tradedate), ''), NULLIF(TRIM(ior_buff.raw_tradedate), '')), COALESCE(NULLIF(TRIM(ior_buff.secboard), ''), NULLIF(TRIM(ior_buff.raw_secboard), '')), COALESCE(NULLIF(TRIM(ior_buff.seccode), ''), NULLIF(TRIM(ior_buff.raw_seccode), '')), COALESCE(NULLIF(TRIM(ior_buff.lotsize), ''), NULLIF(TRIM(ior_buff.raw_lotsize), '')), COALESCE(NULLIF(TRIM(ior_buff.minstep), ''), NULLIF(TRIM(ior_buff.raw_minstep), '')), COALESCE(NULLIF(TRIM(ior_buff.facevalue), ''), NULLIF(TRIM(ior_buff.raw_facevalue), '')), COALESCE(NULLIF(TRIM(ior_buff.faceunit), ''), NULLIF(TRIM(ior_buff.raw_faceunit), '')), COALESCE(NULLIF(TRIM(ior_buff.prevprice), ''), NULLIF(TRIM(ior_buff.raw_prevprice), '')), COALESCE(NULLIF(TRIM(ior_buff.decimals), ''), NULLIF(TRIM(ior_buff.raw_decimals), '')), COALESCE(NULLIF(TRIM(ior_buff.prevwaprice), ''), NULLIF(TRIM(ior_buff.raw_prevwaprice), '')), COALESCE(NULLIF(TRIM(ior_buff.open_), ''), NULLIF(TRIM(ior_buff.raw_open_), '')), COALESCE(NULLIF(TRIM(ior_buff.high), ''), NULLIF(TRIM(ior_buff.raw_high), '')), COALESCE(NULLIF(TRIM(ior_buff.low), ''), NULLIF(TRIM(ior_buff.raw_low), '')), COALESCE(NULLIF(TRIM(ior_buff.last_), ''), NULLIF(TRIM(ior_buff.raw_last_), '')), COALESCE(NULLIF(TRIM(ior_buff.change), ''), NULLIF(TRIM(ior_buff.raw_change), '')), COALESCE(NULLIF(TRIM(ior_buff.voltoday), ''), NULLIF(TRIM(ior_buff.raw_voltoday), '')), COALESCE(NULLIF(TRIM(ior_buff.valtoday), ''), NULLIF(TRIM(ior_buff.raw_valtoday), '')), COALESCE(NULLIF(TRIM(ior_buff.value_), ''), NULLIF(TRIM(ior_buff.raw_value), '')), COALESCE(NULLIF(TRIM(ior_buff.waprice), ''), NULLIF(TRIM(ior_buff.raw_waprice), '')), COALESCE(NULLIF(TRIM(ior_buff.numtrades), ''), NULLIF(TRIM(ior_buff.raw_numtrades), '')), COALESCE(NULLIF(TRIM(ior_buff.closeprice), ''), NULLIF(TRIM(ior_buff.raw_closeprice), '')), COALESCE(NULLIF(TRIM(ior_buff.baseprice), ''), NULLIF(TRIM(ior_buff.raw_baseprice), '')), ior_buff.raw_tradedate, ior_buff.raw_secboard, ior_buff.raw_seccode, ior_buff.raw_lotsize, ior_buff.raw_minstep, ior_buff.raw_facevalue, ior_buff.raw_faceunit, ior_buff.raw_prevprice, ior_buff.raw_decimals, ior_buff.raw_prevwaprice, ior_buff.raw_open_, ior_buff.raw_high, ior_buff.raw_low, ior_buff.raw_last_, ior_buff.raw_change, ior_buff.raw_voltoday, ior_buff.raw_valtoday, ior_buff.raw_value, ior_buff.raw_waprice, ior_buff.raw_numtrades, ior_buff.raw_closeprice, ior_buff.raw_baseprice, COALESCE(ior_buff.text, ''), ior_buff.raw_secname, COALESCE(NULLIF(TRIM(ior_buff.secname), ''), NULLIF(TRIM(ior_buff.raw_secname), '')), COALESCE(NULLIF(TRIM(ior_buff.bid), ''), NULLIF(TRIM(ior_buff.raw_bid), '')), ior_buff.raw_bid, COALESCE(NULLIF(TRIM(ior_buff.biddepth), ''), NULLIF(TRIM(ior_buff.raw_biddepth), '')), ior_buff.raw_biddepth, COALESCE(NULLIF(TRIM(ior_buff.numbids), ''), NULLIF(TRIM(ior_buff.raw_numbids), '')), ior_buff.raw_numbids, COALESCE(NULLIF(TRIM(ior_buff.offer), ''), NULLIF(TRIM(ior_buff.raw_offer), '')), ior_buff.raw_offer, COALESCE(NULLIF(TRIM(ior_buff.offerdepth), ''), NULLIF(TRIM(ior_buff.raw_offerdepth), '')), ior_buff.raw_offerdepth, COALESCE(NULLIF(TRIM(ior_buff.offerdeptht), ''), NULLIF(TRIM(ior_buff.raw_offerdeptht), '')), ior_buff.raw_offerdeptht, COALESCE(NULLIF(TRIM(ior_buff.numoffers), ''), NULLIF(TRIM(ior_buff.raw_numoffers), '')), ior_buff.raw_numoffers, COALESCE(NULLIF(TRIM(ior_buff.qty), ''), NULLIF(TRIM(ior_buff.raw_qty), '')), ior_buff.raw_qty, COALESCE(NULLIF(TRIM(ior_buff.highbid), ''), NULLIF(TRIM(ior_buff.raw_highbid), '')), ior_buff.raw_highbid, COALESCE(NULLIF(TRIM(ior_buff.lowoffer), ''), NULLIF(TRIM(ior_buff.raw_lowoffer), '')), ior_buff.raw_lowoffer, COALESCE(NULLIF(TRIM(ior_buff.priceminusprevwaprice), ''), NULLIF(TRIM(ior_buff.raw_priceminusprevwaprice), '')), ior_buff.raw_priceminusprevwaprice, COALESCE(NULLIF(TRIM(ior_buff.numnegdeals), ''), NULLIF(TRIM(ior_buff.raw_numnegdeals), '')), ior_buff.raw_numnegdeals, ior_buff.raw_time, COALESCE(NULLIF(TRIM(ior_buff.marketcode), ''), NULLIF(TRIM(ior_buff.raw_marketcode), '')), ior_buff.raw_marketcode, COALESCE(NULLIF(TRIM(ior_buff.tradingstatus), ''), NULLIF(TRIM(ior_buff.raw_tradingstatus), '')), ior_buff.raw_tradingstatus, COALESCE(NULLIF(TRIM(ior_buff.status), ''), NULLIF(TRIM(ior_buff.raw_status), '')), ior_buff.raw_status, COALESCE(NULLIF(TRIM(ior_buff.remarks), ''), NULLIF(TRIM(ior_buff.raw_remarks), '')), ior_buff.raw_remarks, COALESCE(NULLIF(TRIM(ior_buff.instrid), ''), NULLIF(TRIM(ior_buff.raw_instrid), '')), ior_buff.raw_instrid, COALESCE(NULLIF(TRIM(ior_buff.biddeptht), ''), NULLIF(TRIM(ior_buff.raw_biddeptht), '')), ior_buff.raw_biddeptht, COALESCE(NULLIF(TRIM(ior_buff.marketpricetoday), ''), NULLIF(TRIM(ior_buff.raw_marketpricetoday), '')), ior_buff.raw_marketpricetoday);
END;
$$;

-- type_buff=6181 tr_buff_micex_lotsize
CREATE OR REPLACE PROCEDURE tr_api_loader.micex_lotsize_data_ins(INOUT ior_buff tr__data_view.v_tr_buff_micex_lotsize)
LANGUAGE plpgsql AS $$
DECLARE
    r_buff_cat tr__data_temp.tr_buff%ROWTYPE;
    v_buff_id NUMERIC;
BEGIN
    r_buff_cat.type_section := ior_buff.type_section;
    r_buff_cat.type_buff := 6181;
    r_buff_cat.type_src := ior_buff.type_src;
    r_buff_cat.type_format := NULL;
    r_buff_cat.insert_datetime := NOW();
    INSERT INTO tr__data_temp.tr_buff (type_section, type_buff, type_format, type_src, insert_datetime)
    VALUES (r_buff_cat.type_section, r_buff_cat.type_buff, r_buff_cat.type_format, r_buff_cat.type_src, r_buff_cat.insert_datetime)
    RETURNING buff_id INTO v_buff_id;
    ior_buff.buff_id := v_buff_id;
    INSERT INTO tr__data_temp.tr_buff_micex_lotsize (id, secboard, seccode, lotsize, text, entry_date, expiry_date, insert_date)
    VALUES (v_buff_id, COALESCE(NULLIF(TRIM(ior_buff.secboard), ''), NULLIF(TRIM(ior_buff.raw_secboard), '')), COALESCE(NULLIF(TRIM(ior_buff.seccode), ''), NULLIF(TRIM(ior_buff.raw_seccode), '')), COALESCE(NULLIF(TRIM(ior_buff.lotsize::text), '')::numeric, NULLIF(TRIM(ior_buff.raw_lotsize::text), '')::numeric), COALESCE(ior_buff.text, ''), COALESCE(ior_buff.entry_date, ior_buff.insert_date, NOW()), ior_buff.expiry_date, COALESCE(ior_buff.insert_date, NOW()));
END;
$$;

-- type_buff DEAL_FX (3x INOUT + swap OUT)
CREATE OR REPLACE PROCEDURE tr_api_loader.micex_deal_fx_data_ins(
    INOUT ior_buff tr__data_view.v_tr_buff_micex_deal_fx,
    INOUT ior_buff_near tr__data_view.v_tr_buff_micex_deal_fx,
    INOUT ior_buff_far tr__data_view.v_tr_buff_micex_deal_fx,
    OUT o_is_swap NUMERIC,
    OUT o_err_message VARCHAR
)
LANGUAGE plpgsql AS $$
DECLARE
    r_buff_cat tr__data_temp.tr_buff%ROWTYPE;
    v_buff_id NUMERIC;
    v_near_ok BOOLEAN := FALSE;
    v_far_ok BOOLEAN := FALSE;
    r tr__data_view.v_tr_buff_micex_deal_fx;
BEGIN
    o_err_message := NULL;
    r := ior_buff;
        IF NOT (r.raw_tradeno IS NULL OR btrim(r.raw_tradeno) = '') THEN
            r_buff_cat.type_section := r.type_section;
            r_buff_cat.type_buff := 2323;
            r_buff_cat.type_format := r.type_format;
            r_buff_cat.type_src := r.type_src;
            r_buff_cat.insert_datetime := NOW();
            INSERT INTO tr__data_temp.tr_buff (type_section, type_buff, type_format, type_src, insert_datetime)
            VALUES (r_buff_cat.type_section, r_buff_cat.type_buff, r_buff_cat.type_format, r_buff_cat.type_src, r_buff_cat.insert_datetime)
            RETURNING buff_id INTO v_buff_id;
            r.buff_id := v_buff_id;
            INSERT INTO tr__data_temp.tr_buff_micex_deal_fx (id, tradeno, orderno, tradedatetime, buysell, brokerref, userid, firmid, cpfirmid, account_, secboard, seccode, price, quantity, value_, settledate, period, settlecode, tradetype, extref, commission, raw_tradeno, raw_orderno, raw_tradetime, raw_buysell, raw_brokerref, raw_userid, raw_firmid, raw_cpfirmid, raw_account_, raw_secboard, raw_seccode, raw_price, raw_quantity, raw_value_, raw_settledate, raw_period, raw_settlecode, raw_tradetype, raw_extref, raw_commission, text, raw_tradedate, parenttradeno, raw_clearingcentercomm, clearingcentercomm, exchangecomm, raw_exchangecomm, tradingsystemcomm, raw_tradingsystemcomm, clientcode, raw_clientcode, parent_buff_id, perm_buff_id, memo_1, memo_2)
            VALUES (v_buff_id, COALESCE(NULLIF(TRIM(r.tradeno), ''), NULLIF(TRIM(r.raw_tradeno), '')), COALESCE(NULLIF(TRIM(r.orderno), ''), NULLIF(TRIM(r.raw_orderno), '')), r.tradedatetime, COALESCE(NULLIF(TRIM(r.buysell), ''), NULLIF(TRIM(r.raw_buysell), '')), COALESCE(NULLIF(TRIM(r.brokerref), ''), NULLIF(TRIM(r.raw_brokerref), '')), COALESCE(NULLIF(TRIM(r.userid), ''), NULLIF(TRIM(r.raw_userid), '')), COALESCE(NULLIF(TRIM(r.firmid), ''), NULLIF(TRIM(r.raw_firmid), '')), COALESCE(NULLIF(TRIM(r.cpfirmid), ''), NULLIF(TRIM(r.raw_cpfirmid), '')), COALESCE(NULLIF(TRIM(r.account_), ''), NULLIF(TRIM(r.raw_account_), '')), COALESCE(NULLIF(TRIM(r.secboard), ''), NULLIF(TRIM(r.raw_secboard), '')), COALESCE(NULLIF(TRIM(r.seccode), ''), NULLIF(TRIM(r.raw_seccode), '')), COALESCE(NULLIF(TRIM(r.price), ''), NULLIF(TRIM(r.raw_price), '')), COALESCE(NULLIF(TRIM(r.quantity), ''), NULLIF(TRIM(r.raw_quantity), '')), COALESCE(NULLIF(TRIM(r.value_), ''), NULLIF(TRIM(r.raw_value_), '')), COALESCE(NULLIF(TRIM(r.settledate), ''), NULLIF(TRIM(r.raw_settledate), '')), COALESCE(NULLIF(TRIM(r.period), ''), NULLIF(TRIM(r.raw_period), '')), COALESCE(NULLIF(TRIM(r.settlecode), ''), NULLIF(TRIM(r.raw_settlecode), '')), COALESCE(NULLIF(TRIM(r.tradetype), ''), NULLIF(TRIM(r.raw_tradetype), '')), COALESCE(NULLIF(TRIM(r.extref), ''), NULLIF(TRIM(r.raw_extref), '')), COALESCE(NULLIF(TRIM(r.commission), ''), NULLIF(TRIM(r.raw_commission), '')), r.raw_tradeno, r.raw_orderno, r.raw_tradetime, r.raw_buysell, r.raw_brokerref, r.raw_userid, r.raw_firmid, r.raw_cpfirmid, r.raw_account_, r.raw_secboard, r.raw_seccode, r.raw_price, r.raw_quantity, r.raw_value_, r.raw_settledate, r.raw_period, r.raw_settlecode, r.raw_tradetype, r.raw_extref, r.raw_commission, COALESCE(r.text, ''), r.raw_tradedate, r.parenttradeno, r.raw_clearingcentercomm, COALESCE(NULLIF(TRIM(r.clearingcentercomm), ''), NULLIF(TRIM(r.raw_clearingcentercomm), '')), COALESCE(NULLIF(TRIM(r.exchangecomm), ''), NULLIF(TRIM(r.raw_exchangecomm), '')), r.raw_exchangecomm, COALESCE(NULLIF(TRIM(r.tradingsystemcomm), ''), NULLIF(TRIM(r.raw_tradingsystemcomm), '')), r.raw_tradingsystemcomm, COALESCE(NULLIF(TRIM(r.clientcode), ''), NULLIF(TRIM(r.raw_clientcode), '')), r.raw_clientcode, r.parent_buff_id, r.perm_buff_id, r.memo_1, r.memo_2);
        END IF;
    r := ior_buff_near;
        IF NOT (r.raw_tradeno IS NULL OR btrim(r.raw_tradeno) = '') THEN
            r_buff_cat.type_section := r.type_section;
            r_buff_cat.type_buff := 2323;
            r_buff_cat.type_format := r.type_format;
            r_buff_cat.type_src := r.type_src;
            r_buff_cat.insert_datetime := NOW();
            INSERT INTO tr__data_temp.tr_buff (type_section, type_buff, type_format, type_src, insert_datetime)
            VALUES (r_buff_cat.type_section, r_buff_cat.type_buff, r_buff_cat.type_format, r_buff_cat.type_src, r_buff_cat.insert_datetime)
            RETURNING buff_id INTO v_buff_id;
            r.buff_id := v_buff_id;
            INSERT INTO tr__data_temp.tr_buff_micex_deal_fx (id, tradeno, orderno, tradedatetime, buysell, brokerref, userid, firmid, cpfirmid, account_, secboard, seccode, price, quantity, value_, settledate, period, settlecode, tradetype, extref, commission, raw_tradeno, raw_orderno, raw_tradetime, raw_buysell, raw_brokerref, raw_userid, raw_firmid, raw_cpfirmid, raw_account_, raw_secboard, raw_seccode, raw_price, raw_quantity, raw_value_, raw_settledate, raw_period, raw_settlecode, raw_tradetype, raw_extref, raw_commission, text, raw_tradedate, parenttradeno, raw_clearingcentercomm, clearingcentercomm, exchangecomm, raw_exchangecomm, tradingsystemcomm, raw_tradingsystemcomm, clientcode, raw_clientcode, parent_buff_id, perm_buff_id, memo_1, memo_2)
            VALUES (v_buff_id, COALESCE(NULLIF(TRIM(r.tradeno), ''), NULLIF(TRIM(r.raw_tradeno), '')), COALESCE(NULLIF(TRIM(r.orderno), ''), NULLIF(TRIM(r.raw_orderno), '')), r.tradedatetime, COALESCE(NULLIF(TRIM(r.buysell), ''), NULLIF(TRIM(r.raw_buysell), '')), COALESCE(NULLIF(TRIM(r.brokerref), ''), NULLIF(TRIM(r.raw_brokerref), '')), COALESCE(NULLIF(TRIM(r.userid), ''), NULLIF(TRIM(r.raw_userid), '')), COALESCE(NULLIF(TRIM(r.firmid), ''), NULLIF(TRIM(r.raw_firmid), '')), COALESCE(NULLIF(TRIM(r.cpfirmid), ''), NULLIF(TRIM(r.raw_cpfirmid), '')), COALESCE(NULLIF(TRIM(r.account_), ''), NULLIF(TRIM(r.raw_account_), '')), COALESCE(NULLIF(TRIM(r.secboard), ''), NULLIF(TRIM(r.raw_secboard), '')), COALESCE(NULLIF(TRIM(r.seccode), ''), NULLIF(TRIM(r.raw_seccode), '')), COALESCE(NULLIF(TRIM(r.price), ''), NULLIF(TRIM(r.raw_price), '')), COALESCE(NULLIF(TRIM(r.quantity), ''), NULLIF(TRIM(r.raw_quantity), '')), COALESCE(NULLIF(TRIM(r.value_), ''), NULLIF(TRIM(r.raw_value_), '')), COALESCE(NULLIF(TRIM(r.settledate), ''), NULLIF(TRIM(r.raw_settledate), '')), COALESCE(NULLIF(TRIM(r.period), ''), NULLIF(TRIM(r.raw_period), '')), COALESCE(NULLIF(TRIM(r.settlecode), ''), NULLIF(TRIM(r.raw_settlecode), '')), COALESCE(NULLIF(TRIM(r.tradetype), ''), NULLIF(TRIM(r.raw_tradetype), '')), COALESCE(NULLIF(TRIM(r.extref), ''), NULLIF(TRIM(r.raw_extref), '')), COALESCE(NULLIF(TRIM(r.commission), ''), NULLIF(TRIM(r.raw_commission), '')), r.raw_tradeno, r.raw_orderno, r.raw_tradetime, r.raw_buysell, r.raw_brokerref, r.raw_userid, r.raw_firmid, r.raw_cpfirmid, r.raw_account_, r.raw_secboard, r.raw_seccode, r.raw_price, r.raw_quantity, r.raw_value_, r.raw_settledate, r.raw_period, r.raw_settlecode, r.raw_tradetype, r.raw_extref, r.raw_commission, COALESCE(r.text, ''), r.raw_tradedate, r.parenttradeno, r.raw_clearingcentercomm, COALESCE(NULLIF(TRIM(r.clearingcentercomm), ''), NULLIF(TRIM(r.raw_clearingcentercomm), '')), COALESCE(NULLIF(TRIM(r.exchangecomm), ''), NULLIF(TRIM(r.raw_exchangecomm), '')), r.raw_exchangecomm, COALESCE(NULLIF(TRIM(r.tradingsystemcomm), ''), NULLIF(TRIM(r.raw_tradingsystemcomm), '')), r.raw_tradingsystemcomm, COALESCE(NULLIF(TRIM(r.clientcode), ''), NULLIF(TRIM(r.raw_clientcode), '')), r.raw_clientcode, r.parent_buff_id, r.perm_buff_id, r.memo_1, r.memo_2);
            v_near_ok := TRUE;
        END IF;
    r := ior_buff_far;
        IF NOT (r.raw_tradeno IS NULL OR btrim(r.raw_tradeno) = '') THEN
            r_buff_cat.type_section := r.type_section;
            r_buff_cat.type_buff := 2323;
            r_buff_cat.type_format := r.type_format;
            r_buff_cat.type_src := r.type_src;
            r_buff_cat.insert_datetime := NOW();
            INSERT INTO tr__data_temp.tr_buff (type_section, type_buff, type_format, type_src, insert_datetime)
            VALUES (r_buff_cat.type_section, r_buff_cat.type_buff, r_buff_cat.type_format, r_buff_cat.type_src, r_buff_cat.insert_datetime)
            RETURNING buff_id INTO v_buff_id;
            r.buff_id := v_buff_id;
            INSERT INTO tr__data_temp.tr_buff_micex_deal_fx (id, tradeno, orderno, tradedatetime, buysell, brokerref, userid, firmid, cpfirmid, account_, secboard, seccode, price, quantity, value_, settledate, period, settlecode, tradetype, extref, commission, raw_tradeno, raw_orderno, raw_tradetime, raw_buysell, raw_brokerref, raw_userid, raw_firmid, raw_cpfirmid, raw_account_, raw_secboard, raw_seccode, raw_price, raw_quantity, raw_value_, raw_settledate, raw_period, raw_settlecode, raw_tradetype, raw_extref, raw_commission, text, raw_tradedate, parenttradeno, raw_clearingcentercomm, clearingcentercomm, exchangecomm, raw_exchangecomm, tradingsystemcomm, raw_tradingsystemcomm, clientcode, raw_clientcode, parent_buff_id, perm_buff_id, memo_1, memo_2)
            VALUES (v_buff_id, COALESCE(NULLIF(TRIM(r.tradeno), ''), NULLIF(TRIM(r.raw_tradeno), '')), COALESCE(NULLIF(TRIM(r.orderno), ''), NULLIF(TRIM(r.raw_orderno), '')), r.tradedatetime, COALESCE(NULLIF(TRIM(r.buysell), ''), NULLIF(TRIM(r.raw_buysell), '')), COALESCE(NULLIF(TRIM(r.brokerref), ''), NULLIF(TRIM(r.raw_brokerref), '')), COALESCE(NULLIF(TRIM(r.userid), ''), NULLIF(TRIM(r.raw_userid), '')), COALESCE(NULLIF(TRIM(r.firmid), ''), NULLIF(TRIM(r.raw_firmid), '')), COALESCE(NULLIF(TRIM(r.cpfirmid), ''), NULLIF(TRIM(r.raw_cpfirmid), '')), COALESCE(NULLIF(TRIM(r.account_), ''), NULLIF(TRIM(r.raw_account_), '')), COALESCE(NULLIF(TRIM(r.secboard), ''), NULLIF(TRIM(r.raw_secboard), '')), COALESCE(NULLIF(TRIM(r.seccode), ''), NULLIF(TRIM(r.raw_seccode), '')), COALESCE(NULLIF(TRIM(r.price), ''), NULLIF(TRIM(r.raw_price), '')), COALESCE(NULLIF(TRIM(r.quantity), ''), NULLIF(TRIM(r.raw_quantity), '')), COALESCE(NULLIF(TRIM(r.value_), ''), NULLIF(TRIM(r.raw_value_), '')), COALESCE(NULLIF(TRIM(r.settledate), ''), NULLIF(TRIM(r.raw_settledate), '')), COALESCE(NULLIF(TRIM(r.period), ''), NULLIF(TRIM(r.raw_period), '')), COALESCE(NULLIF(TRIM(r.settlecode), ''), NULLIF(TRIM(r.raw_settlecode), '')), COALESCE(NULLIF(TRIM(r.tradetype), ''), NULLIF(TRIM(r.raw_tradetype), '')), COALESCE(NULLIF(TRIM(r.extref), ''), NULLIF(TRIM(r.raw_extref), '')), COALESCE(NULLIF(TRIM(r.commission), ''), NULLIF(TRIM(r.raw_commission), '')), r.raw_tradeno, r.raw_orderno, r.raw_tradetime, r.raw_buysell, r.raw_brokerref, r.raw_userid, r.raw_firmid, r.raw_cpfirmid, r.raw_account_, r.raw_secboard, r.raw_seccode, r.raw_price, r.raw_quantity, r.raw_value_, r.raw_settledate, r.raw_period, r.raw_settlecode, r.raw_tradetype, r.raw_extref, r.raw_commission, COALESCE(r.text, ''), r.raw_tradedate, r.parenttradeno, r.raw_clearingcentercomm, COALESCE(NULLIF(TRIM(r.clearingcentercomm), ''), NULLIF(TRIM(r.raw_clearingcentercomm), '')), COALESCE(NULLIF(TRIM(r.exchangecomm), ''), NULLIF(TRIM(r.raw_exchangecomm), '')), r.raw_exchangecomm, COALESCE(NULLIF(TRIM(r.tradingsystemcomm), ''), NULLIF(TRIM(r.raw_tradingsystemcomm), '')), r.raw_tradingsystemcomm, COALESCE(NULLIF(TRIM(r.clientcode), ''), NULLIF(TRIM(r.raw_clientcode), '')), r.raw_clientcode, r.parent_buff_id, r.perm_buff_id, r.memo_1, r.memo_2);
            v_far_ok := TRUE;
        END IF;
    o_is_swap := CASE WHEN v_near_ok AND v_far_ok THEN 81 ELSE 0 END;
EXCEPTION WHEN OTHERS THEN
    o_is_swap := 0;
    o_err_message := SQLERRM;
END;
$$;

