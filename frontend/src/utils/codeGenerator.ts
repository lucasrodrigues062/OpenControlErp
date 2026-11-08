/**
 * Utility for automatic code and identifier generation across OpenControl ERP.
 * Ensures the end user never has to manually invent or type identifiers,
 * while still providing the freedom to edit and customize as desired.
 */

export const generateCode = (prefix: string, length = 5): string => {
  const chars = '0123456789';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return `${prefix.toUpperCase()}-${result}`;
};

export const generateProductSku = (): string => generateCode('PRD', 5);

export const generateCategoryCode = (): string => generateCode('CAT', 4);

export const generateWarehouseCode = (): string => generateCode('DEP', 3);

export const generatePriceListCode = (): string => generateCode('TAB', 3);

export const generateBranchCode = (): string => generateCode('FIL', 3);

export const generateCustomerCategoryCode = (): string => generateCode('CAT', 3);

export const generateEconomicGroupCode = (): string => generateCode('GRP', 3);

export const generatePipelineCode = (): string => generateCode('PIPE', 3);

export const generateLotNumber = (): string => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  const rand = Math.floor(100 + Math.random() * 900);
  return `LOT-${year}${month}${day}-${rand}`;
};

export const generateSerialNumber = (): string => {
  const year = new Date().getFullYear();
  const rand = Math.floor(100000 + Math.random() * 900000);
  return `SN-${year}-${rand}`;
};
