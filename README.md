# database-init
Initialization script for the Opportunities database

# How to use
------------------
1. Install mongo<br />
  <code> $ brew install mongo </code>
2. Run mongo<br />
  <code> $ mongod </code>
3. Navigate to root directory of repository
4. Execute the script<br />
  With Maven:<br />
  <code> $ mvn spring-boot:run -Drun.arguments="mongodb://user:pass@host:port/database,data.tsv"</code><br />
  Without Maven:<br />
  <code> $ java -cp target/database-init-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.script.dbinit.DatabaseInitMain mongodb://user:pass@host:port/database data.tsv
</code><br />
5.  e.g Execute it locally<br />
  <code> $ java -cp target/database-init-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.script.dbinit.DatabaseInitMain mongodb://localhost:27017/database opportunities.tsv </code>



