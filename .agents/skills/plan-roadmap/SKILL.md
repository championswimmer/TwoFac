---
name: Plans and Roadmapping
description: How to create plans and follow roadmaps
---

# Plans and Roadmapping

Long running major projects/changes to this repository are done via creating plans and tracking their roadmap. 

## Directory 

Plans are stored in the `.agents/plans` directory. 
Plans are named like 

## Plan Format

Plans are stored in markdown files with the following format:

```markdown
---
name: Plan Name
status: Planned # Planned, In Progress, Completed
progress:
  - "[ ] Task 1" # mark with [x] when done
  - "[ ] Task 2"
  - "[ ] Task 3"
---

# Plan Name

Plan description and research .....
```

## Working with Plans

1. When asked to plan before starting a project, create a new plan in the `.agents/plans` directory.
2. When asked to implement a plan, check how much of the plan is already completed, and confirm from user which stages they want to implement next or if they want to implement the entire plan. 
3. As steps of the plan are completed, mark them as done in the plan file. 
4. While working with the plan, if more steps are discovered, add to the roadmap items in the plan.
