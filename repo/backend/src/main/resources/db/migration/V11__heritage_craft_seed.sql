-- Update categories to heritage crafts
UPDATE category SET name = 'Pottery',    description = 'Handmade ceramics and stoneware' WHERE id = 1;
UPDATE category SET name = 'Textiles',   description = 'Woven fabrics, quilts, and tapestries' WHERE id = 2;
UPDATE category SET name = 'Woodwork',   description = 'Carved and joined wood pieces' WHERE id = 3;
UPDATE category SET name = 'Metalwork',  description = 'Forged and cast metal artisanry' WHERE id = 4;

-- Update products to heritage craft items
UPDATE product SET name = 'Raku-Fired Tea Bowl',        description = 'Hand-thrown raku tea bowl with copper glaze',     price = 85.00,  stock_quantity = 12  WHERE id = 1;
UPDATE product SET name = 'Indigo Shibori Wall Hanging', description = 'Natural indigo dyed cotton shibori tapestry',    price = 220.00, stock_quantity = 8   WHERE id = 2;
UPDATE product SET name = 'Dovetail Jewelry Box',        description = 'Black walnut jewelry box with hand-cut dovetails', price = 175.00, stock_quantity = 15  WHERE id = 3;
UPDATE product SET name = 'Hand-Knit Merino Scarf',      description = 'Cable-knit merino wool scarf in heather grey',   price = 65.00,  stock_quantity = 25  WHERE id = 4;
UPDATE product SET name = 'Copper Serving Tray',         description = 'Hammered copper tray with patina finish',         price = 145.00, stock_quantity = 6   WHERE id = 5;
UPDATE product SET name = 'Turned Maple Salad Bowl',     description = 'Lathe-turned sugar maple bowl with food-safe finish', price = 120.00, stock_quantity = 10 WHERE id = 6;

-- Update listings to match craft items
UPDATE listing SET title = 'Raku Tea Bowl - Copper Glaze',       slug = 'raku-tea-bowl-copper-glaze',       summary = 'Unique hand-thrown raku fired tea bowl',        tags = ARRAY['pottery','raku','tea','ceramic'],       neighborhood = 'Arts District', sqft = 200,  layout = 'studio',     price = 85.00  WHERE id = 1;
UPDATE listing SET title = 'Indigo Shibori Tapestry',            slug = 'indigo-shibori-tapestry',            summary = 'Traditional Japanese dye technique on cotton',   tags = ARRAY['textiles','shibori','indigo','wall-art'],  neighborhood = 'Craft Quarter', sqft = 350,  layout = 'workshop',   price = 220.00 WHERE id = 2;
UPDATE listing SET title = 'Black Walnut Jewelry Box',           slug = 'black-walnut-jewelry-box',           summary = 'Heirloom-quality dovetail joinery',             tags = ARRAY['woodwork','walnut','jewelry','box'],      neighborhood = 'Old Town',      sqft = 500,  layout = 'workshop',   price = 175.00 WHERE id = 3;
UPDATE listing SET title = 'Merino Cable-Knit Scarf',            slug = 'merino-cable-knit-scarf',            summary = 'Cozy hand-knit merino wool accessory',          tags = ARRAY['textiles','knit','merino','scarf'],       neighborhood = 'Arts District', sqft = 150,  layout = 'studio',     price = 65.00  WHERE id = 4;
UPDATE listing SET title = 'Hammered Copper Serving Tray',       slug = 'hammered-copper-serving-tray',       summary = 'Artisan hammered copper with natural patina',    tags = ARRAY['metalwork','copper','tray','hammered'],   neighborhood = 'Foundry Row',   sqft = 800,  layout = 'workshop',   price = 145.00 WHERE id = 5;
UPDATE listing SET title = 'Maple Salad Bowl - Lathe Turned',    slug = 'maple-salad-bowl-lathe-turned',      summary = 'Food-safe turned sugar maple serving bowl',     tags = ARRAY['woodwork','maple','bowl','turned'],       neighborhood = 'Old Town',      sqft = 500,  layout = 'workshop',   price = 120.00 WHERE id = 6;
