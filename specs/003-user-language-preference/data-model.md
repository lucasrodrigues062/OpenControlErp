# Data Model: Frontend Multi-Language Support & User Language Preference

**Feature**: `003-user-language-preference`  
**Date**: 2026-11-02  
**Status**: Ready  

---

## 1. Database Schema Extension (PostgreSQL / Flyway `V1_0_6`)

### Table: `iam_user` (Alteration)

```sql
ALTER TABLE iam_user
    ADD COLUMN preferred_language VARCHAR(10) NOT NULL DEFAULT 'en-US';

COMMENT ON COLUMN iam_user.preferred_language IS 'ISO language tag for user preferred UI language (e.g. en-US, pt-BR)';
```

### Entity: `User` (Kotlin JPA)

```kotlin
@Entity
@Table(name = "iam_user")
class User(
    // ... existing fields ...
    @Column(name = "preferred_language", nullable = false)
    var preferredLanguage: String = "en-US",
) : BaseEntity()
```

---

## 2. API Data Transfer Objects (DTOs)

### `UserPreferencesDto`
```kotlin
data class UserPreferencesDto(
    @field:Pattern(regexp = "^(en-US|pt-BR)$", message = "Supported languages are en-US or pt-BR")
    val preferredLanguage: String,
)
```

### Updated `UserResponseDto` / `UserSummaryDto`
```kotlin
data class UserResponseDto(
    val id: UUID,
    val username: String,
    val email: String,
    val fullName: String,
    val preferredLanguage: String,
    val status: String,
    val profile: ProfileSummaryDto,
    val createdAt: Instant,
)
```

---

## 3. Frontend Data Models (TypeScript)

### `SupportedLanguage`
```typescript
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
```

### Translation Dictionary Namespaces

```typescript
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
}

export interface AuthTranslations {
  title: string;
  subtitle: string;
  username: string;
  password: string;
  signIn: string;
  signingIn: string;
  invalidCredentials: string;
}

export interface IamTranslations {
  usersTitle: string;
  usersDescription: string;
  createUser: string;
  editUser: string;
  fullName: string;
  email: string;
  profile: string;
  profilesTitle: string;
  profilesDescription: string;
  createProfile: string;
  editProfile: string;
  profileName: string;
  description: string;
  permissions: string;
  allowed: string;
  restricted: string;
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
}

export interface TranslationDictionary {
  common: CommonTranslations;
  auth: AuthTranslations;
  iam: IamTranslations;
  audit: AuditTranslations;
}
```
