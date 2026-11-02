export type SupportedLanguage = 'en-US' | 'pt-BR';

export interface LanguageDescriptor {
  code: SupportedLanguage;
  label: string;
  flag: string;
}

export const SUPPORTED_LANGUAGES: LanguageDescriptor[] = [
  { code: 'en-US', label: 'English (US)', flag: '🇺🇸' },
  { code: 'pt-BR', label: 'Português (Brasil)', flag: '🇧🇷' },
];

export interface CommonTranslations {
  save: string;
  cancel: string;
  delete: string;
  edit: string;
  create: string;
  search: string;
  filter: string;
  status: string;
  actions: string;
  loading: string;
  active: string;
  inactive: string;
  system: string;
  noData: string;
  signOut: string;
  dashboard: string;
  language: string;
  noAccessibleModules: string;
  selectLanguage: string;
  all: string;
  yes: string;
  no: string;
  jurisdiction: string;
}

export interface AuthTranslations {
  title: string;
  subtitle: string;
  username: string;
  password: string;
  signIn: string;
  signingIn: string;
  invalidCredentials: string;
  usernamePlaceholder: string;
  passwordPlaceholder: string;
}

export interface IamTranslations {
  usersTitle: string;
  usersDescription: string;
  createUser: string;
  editUser: string;
  fullName: string;
  fullNamePlaceholder: string;
  email: string;
  emailPlaceholder: string;
  profile: string;
  selectProfile: string;
  profilesTitle: string;
  profilesDescription: string;
  createProfile: string;
  editProfile: string;
  profileName: string;
  profileNamePlaceholder: string;
  description: string;
  descriptionPlaceholder: string;
  permissions: string;
  allowed: string;
  restricted: string;
  cannotDeleteSystemProfile: string;
  userCreated: string;
  userUpdated: string;
  profileCreated: string;
  profileUpdated: string;
}

export interface AuditTranslations {
  title: string;
  description: string;
  timestamp: string;
  actor: string;
  action: string;
  entityType: string;
  entityId: string;
  stateBefore: string;
  stateAfter: string;
  noChanges: string;
  details: string;
  inspectChanges: string;
  viewDiff: string;
  filterByEntity: string;
  filterByAction: string;
  allEntities: string;
  allActions: string;
}

export interface RulesTranslations {
  title: string;
  description: string;
  createRule: string;
  editRule: string;
  deleteRule: string;
  ruleName: string;
  ruleNamePlaceholder: string;
  ruleDescription: string;
  ruleDescriptionPlaceholder: string;
  trigger: string;
  selectTrigger: string;
  timing: string;
  authoringMode: string;
  visualMode: string;
  dslMode: string;
  condition: string;
  action: string;
  actionType: string;
  actionPayload: string;
  actionPayloadPlaceholder: string;
  priority: string;
  policy: string;
  strictPolicy: string;
  permissivePolicy: string;
  testRule: string;
  simulateBtn: string;
  simulating: string;
  simulationTitle: string;
  simulationDescription: string;
  contextJson: string;
  simulatedResult: string;
  matched: string;
  notMatched: string;
  executionError: string;
  duration: string;
  addCondition: string;
  removeCondition: string;
  field: string;
  operator: string;
  value: string;
  viewTraces: string;
  tracesTitle: string;
  tracesDescription: string;
  noRules: string;
  noTraces: string;
  ruleCreated: string;
  ruleUpdated: string;
  ruleDeleted: string;
  toggleSuccess: string;
  expand: string;
  collapse: string;
  fullscreen: string;
  normalView: string;
  addGroup: string;
  generatedLogic: string;
  fieldSuggestions: string;
  searchField: string;
  browseFields: string;
  fieldCatalog: string;
  fieldCatalogDesc: string;
  allEntities: string;
  dataType: string;
  example: string;
}

export interface TranslationDictionary {
  common: CommonTranslations;
  auth: AuthTranslations;
  iam: IamTranslations;
  audit: AuditTranslations;
  rules: RulesTranslations;
  customFields: import('../locales/en-US/customFields').CustomFieldsTranslations;
}
