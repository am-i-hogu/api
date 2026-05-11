INSERT INTO policy_revisions
    (id, policy_type, version, html_content, is_current, updated_at)
VALUES (
        1,
        'PRIVACY',
        'v1.0.0',
        "<h1>개인정보 처리 방침</h1><p>이전 버전 내용</p>",
        false,
        '2026-01-01 09:00:00'
         ),
         (
          2,
          'PRIVACY',
          'v1.0.1',
          "<h1>개인정보 처리 방침</h1><p>최신 버전 내용</p>",
          true,
          '2026-05-01 09:00:00'
         );