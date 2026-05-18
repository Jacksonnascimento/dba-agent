package com.dbaagent.api.services.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DdlSchemaIndexTest {

    @Test
    void parsesTableAndColumns() {
        String ddl = """
                CREATE TABLE GER_HISTORICO_TENTATIVA_ACESSO (
                HTA_ID int,
                HTA_USUARIO varchar(50)
                , PRIMARY KEY (HTA_ID)
                );
                CREATE TABLE FPG_FERIAS (FUN_COD int, FER_INICIO datetime);
                """;
        DdlSchemaIndex index = DdlSchemaIndex.parse(ddl);
        assertTrue(index.hasTable("GER_HISTORICO_TENTATIVA_ACESSO"));
        assertTrue(index.hasColumn("GER_HISTORICO_TENTATIVA_ACESSO", "HTA_USUARIO"));
        assertFalse(index.hasColumn("GER_HISTORICO_TENTATIVA_ACESSO", "HTA_DATA"));
    }
}
