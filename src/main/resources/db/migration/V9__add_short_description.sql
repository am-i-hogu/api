-- add short_description column
ALTER TABLE hogu_levels
    ADD COLUMN short_description TEXT NOT NULL;

UPDATE hogu_levels
SET short_description = CASE code
    WHEN 'SAFE' THEN '쉽게 휘둘리지 않는 타입'
    WHEN 'CAUTIOUS' THEN '대체로 균형 잡힌 타입'
    WHEN 'WARNING' THEN '가끔 손해를 감수하는 타입'
    WHEN 'RISKY' THEN '거절보다 양보가 앞서는 타입'
    WHEN 'CRITICAL' THEN '반복 손해 위험이 높은 타입'
END;
