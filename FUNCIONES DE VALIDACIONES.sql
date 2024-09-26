-- FUNCTION: adempiere.coma_concat(text, text)

-- DROP FUNCTION IF EXISTS adempiere.coma_concat(text, text);

CREATE OR REPLACE FUNCTION adempiere.coma_concat(
	text,
	text)
    RETURNS text
    LANGUAGE 'sql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$

    SELECT
    CASE
        WHEN $2 IS NULL OR $2 = '' THEN '[' ||  $1 || ']'
        WHEN $1 IS NULL OR $1 = '' THEN '[' ||  $2 || ']'
        ELSE $1 || ';' ||   '[' || $2 || ']'

    END
    
$BODY$;

ALTER FUNCTION adempiere.coma_concat(text, text)
    OWNER TO adempiere;





-- Aggregate: list_horizontal;

-- DROP AGGREGATE IF EXISTS adempiere.list_horizontal(text);

CREATE OR REPLACE AGGREGATE adempiere.list_horizontal(text) (
    SFUNC = coma_concat,
    STYPE = text ,
    FINALFUNC_MODIFY = READ_ONLY,
    MFINALFUNC_MODIFY = READ_ONLY
);






-- FUNCTION: adempiere.fnt_validar_factura_varios_pagos(numeric)

-- DROP FUNCTION IF EXISTS adempiere.fnt_validar_factura_varios_pagos(numeric);

CREATE OR REPLACE FUNCTION adempiere.fnt_validar_factura_varios_pagos(
	p_invoice_id numeric)
    RETURNS boolean
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$

DECLARE
	v_resultado INT = 0;
BEGIN
	v_resultado =
	(
		WITH cte_factura_pagos AS
		(
			SELECT
				p.c_payment_id AS id_pago,
				pa.c_paymentallocate_id AS id_abono,
				pa.c_invoice_id AS id_factura,
				pa.overunderamt AS monto_subpago
			FROM c_payment p
				JOIN c_paymentallocate pa
				ON p.c_payment_id = pa.c_payment_id
				
			WHERE p.ad_client_id = 1000000
				AND p.docstatus IN ('CO', 'CL', 'DR', 'IN')
			AND pa.c_invoice_id = p_invoice_id
				
				and p.reversal_id is null
		)
		SELECT COALESCE(
		(
			SELECT DISTINCT 1 FROM cte_factura_pagos fp
			WHERE
			((
				SELECT
					COUNT(cp.id_factura) + 1
				FROM cte_factura_pagos AS cp
        		
			) > 1
			AND (
				SELECT
					fr.monto_subpago
				FROM cte_factura_pagos fr
				WHERE fr.id_abono = 
				(
					SELECT
						MAX(r.id_abono)
					FROM cte_factura_pagos AS r
				)
			) = 0)
		), 0)
	);
	
	IF(v_resultado = 1)THEN
		RETURN FALSE;
	ELSE
		RETURN TRUE;
	END IF;
END;
$BODY$;

ALTER FUNCTION adempiere.fnt_validar_factura_varios_pagos(numeric)
    OWNER TO adempiere;
    
    
    
    
    
    
    
    -- FUNCTION: adempiere.fnt_obtener_vpagos_factura(numeric)

-- DROP FUNCTION IF EXISTS adempiere.fnt_obtener_vpagos_factura(numeric);

CREATE OR REPLACE FUNCTION adempiere.fnt_obtener_vpagos_factura(
	p_invoice_id numeric)
    RETURNS character varying
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$

BEGIN
	RETURN
	(
		WITH cte_factura_pagos AS
		(
			SELECT
				p.c_payment_id AS id_pago,
				p.documentno AS num_pago,
				pa.c_paymentallocate_id AS id_abono,
				pa.c_invoice_id AS id_factura,
				pa.overunderamt AS monto_subpago
			FROM c_payment p
				JOIN c_paymentallocate pa
				ON p.c_payment_id = pa.c_payment_id
				
			WHERE p.ad_client_id = 1000000
				AND p.docstatus IN ('CL', 'DR', 'IN')
				AND pa.c_invoice_id = p_invoice_id
				
				and p.reversal_id is null
		)
		SELECT COALESCE(
		(
			SELECT list_horizontal(fp.num_pago)::VARCHAR FROM cte_factura_pagos fp
			WHERE
			((
				SELECT
					COUNT(cp.id_factura) + 1
				FROM cte_factura_pagos AS cp
        		
			) > 1
			AND (
				SELECT
					fr.monto_subpago
				FROM cte_factura_pagos fr
				WHERE fr.id_abono = 
				(
					SELECT
						MAX(r.id_abono)
					FROM cte_factura_pagos AS r
				)
			) = 0)
		), '')
	);
END;
$BODY$;

ALTER FUNCTION adempiere.fnt_obtener_vpagos_factura(numeric)
    OWNER TO adempiere;







-- FUNCTION: adempiere.fnt_validar_pago_facturas_anuladas(numeric)

-- DROP FUNCTION IF EXISTS adempiere.fnt_validar_pago_facturas_anuladas(numeric);

CREATE OR REPLACE FUNCTION adempiere.fnt_validar_pago_facturas_anuladas(
	p_payment_id numeric)
    RETURNS integer
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$

DECLARE
	v_anulado int = 0;
BEGIN
	v_anulado =
	(
	SELECT COALESCE((
	SELECT
		1
	FROM c_payment p
		JOIN c_paymentallocate pa
		ON p.c_payment_id = pa.c_payment_id
		JOIN c_invoice AS f
		ON pa.c_invoice_id = f.c_invoice_id
	WHERE p.c_payment_id = p_payment_id
		AND f.docstatus IN ('VO', 'CL', 'RE')), 0)
		);
		
		return v_anulado;
END;
$BODY$;

ALTER FUNCTION adempiere.fnt_validar_pago_facturas_anuladas(numeric)
    OWNER TO adempiere;




-- FUNCTION: adempiere.fnt_obtener_facturas_anuladas_pago(numeric)

-- DROP FUNCTION IF EXISTS adempiere.fnt_obtener_facturas_anuladas_pago(numeric);

CREATE OR REPLACE FUNCTION adempiere.fnt_obtener_facturas_anuladas_pago(
	p_payment_id numeric)
    RETURNS character varying
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$

DECLARE
	v_num_factura varchar = '0';
BEGIN
	v_num_factura =
	(
	SELECT COALESCE((
	SELECT
		f.documentno
	FROM c_payment p
		JOIN c_paymentallocate pa
		ON p.c_payment_id = pa.c_payment_id
		JOIN c_invoice AS f
		ON pa.c_invoice_id = f.c_invoice_id
	WHERE p.c_payment_id = p_payment_id
		AND f.docstatus IN ('VO', 'CL', 'RE') LIMIT 1), '0')
		);
		
		return v_num_factura;
END;
$BODY$;

ALTER FUNCTION adempiere.fnt_obtener_facturas_anuladas_pago(numeric)
    OWNER TO adempiere;


