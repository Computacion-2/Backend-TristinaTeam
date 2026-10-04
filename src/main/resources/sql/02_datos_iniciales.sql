-- =====================================================
-- CUIDAPET - DATOS INICIALES
-- Ejecutar con el usuario CUIDAPET en Oracle XEPDB1
-- =====================================================


-- =====================================================
-- 1. PERMISOS
-- =====================================================

INSERT INTO APP_PERMISSION
(PERMISSION_CODE, PERMISSION_NAME, DESCRIPTION, CREATED_AT)
SELECT
    'MANAGE_USERS',
    'Gestionar usuarios',
    'Permite administrar los usuarios del sistema',
    SYSTIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM APP_PERMISSION
    WHERE PERMISSION_CODE = 'MANAGE_USERS'
);

INSERT INTO APP_PERMISSION
(PERMISSION_CODE, PERMISSION_NAME, DESCRIPTION, CREATED_AT)
SELECT
    'MANAGE_ROLES',
    'Gestionar roles',
    'Permite administrar los roles del sistema',
    SYSTIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM APP_PERMISSION
    WHERE PERMISSION_CODE = 'MANAGE_ROLES'
);

INSERT INTO APP_PERMISSION
(PERMISSION_CODE, PERMISSION_NAME, DESCRIPTION, CREATED_AT)
SELECT
    'CREATE_REQUEST',
    'Crear solicitudes',
    'Permite crear solicitudes de cuidado',
    SYSTIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM APP_PERMISSION
    WHERE PERMISSION_CODE = 'CREATE_REQUEST'
);

INSERT INTO APP_PERMISSION
(PERMISSION_CODE, PERMISSION_NAME, DESCRIPTION, CREATED_AT)
SELECT
    'EDIT_REQUEST',
    'Editar solicitudes',
    'Permite editar solicitudes de cuidado',
    SYSTIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM APP_PERMISSION
    WHERE PERMISSION_CODE = 'EDIT_REQUEST'
);


-- =====================================================
-- 2. ROLES
-- =====================================================

INSERT INTO APP_ROLE
(ROLE_NAME, DESCRIPTION, ACTIVE_FLAG, CREATED_AT)
SELECT
    'ADMIN',
    'Administrador del sistema',
    'Y',
    SYSTIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM APP_ROLE
    WHERE ROLE_NAME = 'ADMIN'
);

INSERT INTO APP_ROLE
(ROLE_NAME, DESCRIPTION, ACTIVE_FLAG, CREATED_AT)
SELECT
    'CAREGIVER',
    'Cuidador de mascotas',
    'Y',
    SYSTIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM APP_ROLE
    WHERE ROLE_NAME = 'CAREGIVER'
);

INSERT INTO APP_ROLE
(ROLE_NAME, DESCRIPTION, ACTIVE_FLAG, CREATED_AT)
SELECT
    'OWNER',
    'Propietario de mascotas',
    'Y',
    SYSTIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM APP_ROLE
    WHERE ROLE_NAME = 'OWNER'
);


-- =====================================================
-- 3. ASIGNAR PERMISOS A LOS ROLES
-- =====================================================

-- ADMIN: gestionar usuarios
INSERT INTO ROLE_PERMISSION (ROLE_ID, PERMISSION_ID, GRANTED_AT)
SELECT r.ROLE_ID, p.PERMISSION_ID, SYSTIMESTAMP
FROM APP_ROLE r
         CROSS JOIN APP_PERMISSION p
WHERE r.ROLE_NAME = 'ADMIN'
  AND p.PERMISSION_CODE = 'MANAGE_USERS'
  AND NOT EXISTS (
    SELECT 1 FROM ROLE_PERMISSION rp
    WHERE rp.ROLE_ID = r.ROLE_ID
      AND rp.PERMISSION_ID = p.PERMISSION_ID
);

-- ADMIN: gestionar roles
INSERT INTO ROLE_PERMISSION (ROLE_ID, PERMISSION_ID, GRANTED_AT)
SELECT r.ROLE_ID, p.PERMISSION_ID, SYSTIMESTAMP
FROM APP_ROLE r
         CROSS JOIN APP_PERMISSION p
WHERE r.ROLE_NAME = 'ADMIN'
  AND p.PERMISSION_CODE = 'MANAGE_ROLES'
  AND NOT EXISTS (
    SELECT 1 FROM ROLE_PERMISSION rp
    WHERE rp.ROLE_ID = r.ROLE_ID
      AND rp.PERMISSION_ID = p.PERMISSION_ID
);

-- CAREGIVER: editar solicitudes
INSERT INTO ROLE_PERMISSION (ROLE_ID, PERMISSION_ID, GRANTED_AT)
SELECT r.ROLE_ID, p.PERMISSION_ID, SYSTIMESTAMP
FROM APP_ROLE r
         CROSS JOIN APP_PERMISSION p
WHERE r.ROLE_NAME = 'CAREGIVER'
  AND p.PERMISSION_CODE = 'EDIT_REQUEST'
  AND NOT EXISTS (
    SELECT 1 FROM ROLE_PERMISSION rp
    WHERE rp.ROLE_ID = r.ROLE_ID
      AND rp.PERMISSION_ID = p.PERMISSION_ID
);

-- OWNER: crear solicitudes
INSERT INTO ROLE_PERMISSION (ROLE_ID, PERMISSION_ID, GRANTED_AT)
SELECT r.ROLE_ID, p.PERMISSION_ID, SYSTIMESTAMP
FROM APP_ROLE r
         CROSS JOIN APP_PERMISSION p
WHERE r.ROLE_NAME = 'OWNER'
  AND p.PERMISSION_CODE = 'CREATE_REQUEST'
  AND NOT EXISTS (
    SELECT 1 FROM ROLE_PERMISSION rp
    WHERE rp.ROLE_ID = r.ROLE_ID
      AND rp.PERMISSION_ID = p.PERMISSION_ID
);


-- =====================================================
-- 4. USUARIOS DE PRUEBA
-- =====================================================

-- Las contraseñas son marcadores de prueba.
-- No corresponden a credenciales válidas de inicio de sesión.

INSERT INTO APP_USER
(EMAIL, PASSWORD_HASH, ACCOUNT_STATUS, CREATED_AT, UPDATED_AT)
SELECT
    'admin.seed@cuidapet.test',
    'NO_LOGIN_SEED_HASH',
    'ACTIVE',
    SYSTIMESTAMP,
    SYSTIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM APP_USER
    WHERE EMAIL = 'admin.seed@cuidapet.test'
);

INSERT INTO APP_USER
(EMAIL, PASSWORD_HASH, ACCOUNT_STATUS, CREATED_AT, UPDATED_AT)
SELECT
    'cuidador.seed@cuidapet.test',
    'NO_LOGIN_SEED_HASH',
    'ACTIVE',
    SYSTIMESTAMP,
    SYSTIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM APP_USER
    WHERE EMAIL = 'cuidador.seed@cuidapet.test'
);


-- =====================================================
-- 5. ASIGNAR ROLES A LOS USUARIOS
-- =====================================================

-- Usuario administrador
INSERT INTO USER_ROLE
(USER_ID, ROLE_ID, ASSIGNED_AT, ASSIGNED_BY)
SELECT
    u.USER_ID,
    r.ROLE_ID,
    SYSTIMESTAMP,
    NULL
FROM APP_USER u
         CROSS JOIN APP_ROLE r
WHERE u.EMAIL = 'admin.seed@cuidapet.test'
  AND r.ROLE_NAME = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM USER_ROLE ur
    WHERE ur.USER_ID = u.USER_ID
      AND ur.ROLE_ID = r.ROLE_ID
);

-- Usuario cuidador
INSERT INTO USER_ROLE
(USER_ID, ROLE_ID, ASSIGNED_AT, ASSIGNED_BY)
SELECT
    u.USER_ID,
    r.ROLE_ID,
    SYSTIMESTAMP,
    NULL
FROM APP_USER u
         CROSS JOIN APP_ROLE r
WHERE u.EMAIL = 'cuidador.seed@cuidapet.test'
  AND r.ROLE_NAME = 'CAREGIVER'
  AND NOT EXISTS (
    SELECT 1 FROM USER_ROLE ur
    WHERE ur.USER_ID = u.USER_ID
      AND ur.ROLE_ID = r.ROLE_ID
);


-- =====================================================
-- 6. ESPECIES DE MASCOTAS
-- =====================================================

INSERT INTO PET_SPECIES (SPECIES_NAME, ACTIVE_FLAG)
SELECT 'Perro', 'Y'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM PET_SPECIES WHERE SPECIES_NAME = 'Perro'
);

INSERT INTO PET_SPECIES (SPECIES_NAME, ACTIVE_FLAG)
SELECT 'Gato', 'Y'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM PET_SPECIES WHERE SPECIES_NAME = 'Gato'
);

INSERT INTO PET_SPECIES (SPECIES_NAME, ACTIVE_FLAG)
SELECT 'Ave', 'Y'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM PET_SPECIES WHERE SPECIES_NAME = 'Ave'
);

INSERT INTO PET_SPECIES (SPECIES_NAME, ACTIVE_FLAG)
SELECT 'Conejo', 'Y'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM PET_SPECIES WHERE SPECIES_NAME = 'Conejo'
);


-- =====================================================
-- 7. TIPOS DE SERVICIO
-- =====================================================

INSERT INTO SERVICE_TYPE
(SERVICE_NAME, DESCRIPTION, ACTIVE_FLAG, CREATED_AT)
SELECT
    'Paseo',
    'Servicio de paseo de mascotas',
    'Y',
    SYSTIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM SERVICE_TYPE WHERE SERVICE_NAME = 'Paseo'
);

INSERT INTO SERVICE_TYPE
(SERVICE_NAME, DESCRIPTION, ACTIVE_FLAG, CREATED_AT)
SELECT
    'Alojamiento',
    'Cuidado de mascotas en el hogar del cuidador',
    'Y',
    SYSTIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM SERVICE_TYPE WHERE SERVICE_NAME = 'Alojamiento'
);

INSERT INTO SERVICE_TYPE
(SERVICE_NAME, DESCRIPTION, ACTIVE_FLAG, CREATED_AT)
SELECT
    'Cuidado a domicilio',
    'Cuidado de mascotas en el hogar del propietario',
    'Y',
    SYSTIMESTAMP
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM SERVICE_TYPE WHERE SERVICE_NAME = 'Cuidado a domicilio'
);


-- =====================================================
-- GUARDAR CAMBIOS
-- =====================================================

COMMIT;