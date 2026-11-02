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

export interface TranslationDictionary {
  common: CommonTranslations;
  auth: AuthTranslations;
  iam: IamTranslations;
  audit: AuditTranslations;
}
