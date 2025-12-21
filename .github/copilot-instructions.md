# Project Overview

This project is a Groovy command-line application that allows users to convert
Spotify playlists to ZaraRadio playlists. Music is automatically downloaded,
silence-trimmed and normalised. The project is built using the most recent
stable version of Gradle.

## Folder Structure

- `src/`: Contains the source code
- `docs/`: Contains documentation for the project, including API specifications
  and user guides.

## Libraries and Frameworks

The most-recent stable library versions are preferred.

- Log4j 2 for error logging.
- JUnit for unit testing.

## Coding Standards

- Use two spaces for indentation.
- Lines wrap at column 80.
- English is the preferred language for all comments, issues, commits, pull
  requests and other communications. Spelling and grammar should use British
  or Irish conventions where possible.
- Unit tests must be derived from groovy.test.GroovyTestCase.
- Use explicit types, do not use def.
- Use Power Assertions in unit tests.
- Unit tests must mock all network access with groovy.mock.interceptor.MockFor.
- Unit tests must mock all file system use with groovy.mock.interceptor.MockFor.
- Method names must be camel cased.
- Variable names must be camel cased.
- Do not use underscores in method names or variable names.

