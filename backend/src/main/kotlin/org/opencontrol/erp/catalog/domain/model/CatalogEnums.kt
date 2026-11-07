package org.opencontrol.erp.catalog.domain.model

/**
 * SPED Fiscal 0200 Item Type Classification.
 */
enum class SpedItemType(
    val code: String,
    val description: String,
) {
    REV_00("00", "Mercadoria para Revenda"),
    MP_01("01", "Matéria-Prima"),
    EMB_02("02", "Embalagem"),
    PP_03("03", "Produto em Processo"),
    PA_04("04", "Produto Acabado"),
    SP_05("05", "Subproduto"),
    PI_06("06", "Produto Intermediário"),
    UC_07("07", "Material de Uso e Consumo"),
    AI_08("08", "Ativo Imobilizado"),
    SRV_09("09", "Serviços"),
    OUT_10("10", "Outros Insumos"),
    OUT_99("99", "Outras"),
    ;

    companion object {
        fun fromCode(code: String): SpedItemType =
            entries.firstOrNull { it.code == code || it.name == code }
                ?: throw IllegalArgumentException("Invalid SPED item type code: $code")
    }
}

/**
 * Product inventory traceability control mode.
 */
enum class ProductTrackingMode {
    NONE,
    BATCH_LOT,
    SERIAL_NUMBER,
}
