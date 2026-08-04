# Contributing to Stock Signal Tracker

Thank you for your interest in contributing to the Stock Signal Tracker project! This document provides guidelines and instructions for contributing.

## Code of Conduct
- Be respectful and inclusive
- Provide constructive feedback
- Focus on code, not on individuals

## Getting Started

### Prerequisites
- Git
- Java 25 (for backend development)
- Node.js 18+ and npm 9+ (for frontend development)
- Docker and Docker Compose (optional, for containerized development)

### Fork and Clone
```bash
git clone https://github.com/suhasd89/stock-signal-tracker.git
cd "Stocks Analyser"
```

### Set Up Development Environment
```bash
# Backend
cd backend
mvn clean install

# Frontend
cd ../frontend
npm install
```

## Development Workflow

### Creating a Branch
```bash
# Create feature branch
git checkout -b feature/your-feature-name

# Create bugfix branch
git checkout -b fix/your-bug-fix

# Create docs branch
git checkout -b docs/your-documentation
```

### Code Style

#### Backend (Java)
- **Indentation**: 4 spaces
- **Line Length**: Prefer ≤120 characters
- **Naming**:
  - Classes: PascalCase
  - Methods/Variables: camelCase
  - Constants: UPPER_SNAKE_CASE
- **Comments**: Only when "why" is non-obvious
- **Imports**: Avoid wildcards, use specific imports

#### Frontend (JavaScript/React)
- **Indentation**: 2 spaces (enforced by Prettier)
- **Format**: Run `npm run format` before committing
- **Linting**: Run `npm run lint:fix` before committing
- **Components**: 
  - PascalCase for component files
  - functional components preferred
  - Use hooks for state management
  - PropTypes for prop validation

### Running Tests

#### Backend
```bash
cd backend
mvn test
```

#### Frontend
```bash
cd frontend
npm test
```

## Commit Guidelines

### Commit Message Format
```
type(scope): subject

body

footer
```

### Types
- **feat**: A new feature
- **fix**: A bug fix
- **docs**: Documentation only changes
- **style**: Changes that don't affect code meaning (formatting, missing semicolons, etc)
- **refactor**: Code change that neither fixes a bug nor adds a feature
- **perf**: Code change that improves performance
- **test**: Adding missing tests or correcting existing tests
- **chore**: Changes to build process, dependencies, tooling, etc

### Examples
```
feat(scanner): add V20 momentum indicator support

- Implement V20 calculation logic
- Add V20 API endpoint
- Update dashboard to display V20 results

Closes #123
```

```
fix(frontend): correct watchlist update button positioning

Adjust flex layout to prevent button overlap on mobile devices.

Fixes #456
```

## Pull Request Process

1. **Update your branch**: `git pull origin main`
2. **Verify tests pass**: Run `mvn test` (backend) and `npm test` (frontend)
3. **Push your branch**: `git push origin feature/your-feature`
4. **Create PR**: Open a pull request on GitHub
5. **Fill template**: Use the provided PR template
6. **Request review**: At least one maintainer review required
7. **Address feedback**: Update your branch based on review comments

### PR Checklist
- [ ] Code follows project style guidelines
- [ ] Tests added/updated for new features
- [ ] Documentation updated
- [ ] Commit messages follow guidelines
- [ ] No breaking changes (or documented in PR)
- [ ] Related issues linked

## Testing Requirements

### For Features
- Unit tests for business logic
- Integration tests for API endpoints
- Manual testing in browser/Postman

### For Bug Fixes
- Regression test (test that would fail without the fix)
- Any related tests updated

### Coverage Goals
- **Backend**: Aim for >70% line coverage for new code
- **Frontend**: Aim for >60% coverage for new components

## Documentation

### When to Update
- New features: Add to README.md or CLAUDE.md
- API changes: Update API documentation in comments
- Setup changes: Update CLAUDE.md setup section
- Architecture changes: Document in CLAUDE.md

### Documentation Format
- Use clear, concise language
- Include code examples where helpful
- Update table of contents if adding new sections
- Use relative links for internal references

## Common Development Tasks

### Adding a New Scanning Strategy
1. Add strategy enum in `backend/src/main/java/.../model/StrategyType.java`
2. Implement logic in `ScannerService`
3. Create controller endpoint in `ScannerController`
4. Add frontend UI in `App.jsx`
5. Add tests for new strategy
6. Document in CLAUDE.md

### Adding a New API Endpoint
1. Create method in appropriate controller
2. Add proper security annotations
3. Document with comments
4. Create tests
5. Update API list in README.md

### Updating Frontend Styling
1. Edit `src/styles.css`
2. Test responsive design: `npm run dev` and resize browser
3. Test light/dark modes
4. Ensure accessibility standards met

## Reporting Issues

### Before Reporting
- Check existing issues/PRs to avoid duplicates
- Ensure you can reproduce the issue
- Gather relevant information (OS, version, error message)

### When Reporting
Use the bug report template:
- Clear title
- Detailed reproduction steps
- Expected vs actual behavior
- Screenshots if applicable
- Environment details
- Error logs/stack traces

## Performance Considerations

### Backend
- Use virtual threads for I/O operations
- Batch database operations
- Consider caching strategies
- Monitor Scanner concurrency settings

### Frontend
- Minimize bundle size
- Lazy load components
- Optimize re-renders (use React.memo, useMemo)
- Test performance with DevTools

## Security

- Don't commit secrets, API keys, or passwords
- Use environment variables for sensitive config
- Follow OWASP guidelines for security features
- Report security issues privately to maintainers
- Keep dependencies updated

## Getting Help

- **Questions**: Check CLAUDE.md first
- **Debugging**: Use provided run configurations
- **Architecture**: Review CLAUDE.md Architecture section
- **Community**: Create a discussion if needed

## Licensing

By contributing, you agree that your contributions will be licensed under the same license as the project.

---

Thank you for contributing! Your help makes this project better.
