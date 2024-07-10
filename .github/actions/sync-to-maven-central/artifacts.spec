{
  "files": [
    {
      "aql": {
        "items.find": {
          "$and": [
            {
              "@build.name": "${buildName}",
              "@build.number": "${buildNumber}",
              "path": {
                "$nmatch": "org/springframework/framework-api/*"
              }
            }
          ]
        }
      },
      "target": "nexus/"
    }
  ]
}
