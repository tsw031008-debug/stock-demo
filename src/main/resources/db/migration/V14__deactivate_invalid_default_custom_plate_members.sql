-- 根据2026-09-03最新股票基础快照，停用V12中已退市或已不在当前股票清单的默认成分关系。
UPDATE stock_custom_plate_member
SET active = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE active = 1
  AND (
      (custom_plate_id = 4 AND stock_code IN ('600387'))
      OR (custom_plate_id = 6 AND stock_code IN ('600837'))
      OR (custom_plate_id = 7 AND stock_code IN (
          '000416', '000616', '000627', '000666', '300309', '600705'
      ))
      OR (custom_plate_id = 8 AND stock_code IN ('000806', '002288'))
      OR (custom_plate_id = 9 AND stock_code IN (
          '000851', '002308', '002417', '300330', '300344', '300379', '600355', '688555'
      ))
      OR (custom_plate_id = 10 AND stock_code IN ('000836', '000851', '600355'))
      OR (custom_plate_id = 11 AND stock_code IN ('002308', '300330', '688086'))
      OR (custom_plate_id = 13 AND stock_code IN ('600696'))
      OR (custom_plate_id = 15 AND stock_code IN ('000861', '002336', '600122', '600306'))
  );
