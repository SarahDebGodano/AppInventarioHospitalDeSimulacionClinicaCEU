
INSERT INTO usuario (nombre) VALUES
    ('Alba'),
    ('Gisela'),
    ('Camila')
ON CONFLICT (nombre) DO NOTHING;

UPDATE usuario SET activo = FALSE WHERE nombre LIKE 'Usuario de prueba %';