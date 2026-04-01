# Task for crew-planner

Create a task breakdown for implementing this request.

## Request

Create a test revamp plan for TwoFac. We need to drop obvious tests (e.g. string mapping) and add tests for complex flows, mocking out sync coordinators and backup providers. The work will be split among parallel subagents for sharedLib, composeApp, cliApp, android, ios, and watch apps. Put the plan in .agents/plans/29-testing-revamp/master-plan.md

## Available Skills

Workers can load these skills on demand during task execution. When creating tasks, you may include a `skills` array with relevant skill names to help workers prioritize which to read.

  agent-browser — Browser automation CLI for AI agents. Use when the user needs to interact with websites, including navigating pages, filling forms, clicking buttons, taking screenshots, extracting data, testing web apps, or automating any browser task. Triggers include requests to "open a website", "fill out a form", "click a button", "take a screenshot", "scrape data from a page", "test this web app", "login to a site", "automate browser actions", or any task requiring programmatic web interaction.
  find-skills — Helps users discover and install agent skills when they ask questions like "how do I do X", "find a skill for X", "is there a skill that can...", or express interest in extending capabilities. This skill should be used when the user is looking for functionality that might exist as an installable skill.


You must follow this sequence strictly:
1) Understand the request
2) Review relevant code/docs/reference resources
3) Produce sequential implementation steps
4) Produce a parallel task graph

Return output in this exact section order and headings:
## 1. PRD Understanding Summary
## 2. Relevant Code/Docs/Resources Reviewed
## 3. Sequential Implementation Steps
## 4. Parallelized Task Graph

In section 4, include both:
- markdown task breakdown
- a `tasks-json` fenced block with task objects containing title, description, dependsOn, and optionally skills (array of skill names from the Available Skills list that are relevant to the task).