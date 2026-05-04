ALTER TABLE image_assets
    ADD CONSTRAINT uk_image_assets_url UNIQUE (url);
