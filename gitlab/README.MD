To integrate CleanThat with Gitlab, you have to request a WebHook:

1. Go to `https://gitlab.com/<organisation>/<repository>/-/hooks`
2. Register a webhook with:

- URL: TODO
- Secret token: TODO (AWS Gateway to AWS SQS seems not to enable forwarding headers)
- Trigger on 'Push events' and 'Merge request events'

Authentication:
One need to allow CleanThat to interact on its repository on its behalf:
- https://docs.gitlab.com/ee/user/project/settings/project_access_tokens.html#enable-or-disable-project-access-token-creation
- Need scope: `write_repository`. TODO Is `api` necessary?

In the UI, all CLeanThat interations will be attached to a Bot User.
https://docs.gitlab.com/ee/user/project/settings/project_access_tokens.html#bot-users-for-projects

> These bot users do not count as licensed seats.

TODO API to register a Token
