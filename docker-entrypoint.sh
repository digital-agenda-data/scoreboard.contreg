#!/bin/bash

DB_HOST=${DB_HOST:-"localhost"}
DB_PORT=${DB_PORT:-"1111"}




function replace_propr() {

  PROPR_FILE=$1
  
  if [ -n "$HOME_URL" ]; then
    sed -i "/^\s*application.homeURL/c\application.homeURL\=${HOME_URL}" $PROPR_FILE
  fi

  sed -i "s/DB_HOST/${DB_HOST}/g" $PROPR_FILE
  sed -i "s/DB_PORT/${DB_PORT}/g" $PROPR_FILE


  if [ -n "$DB_USER" ]; then
    sed -i "s/DB_USER/${DB_USER}/g" $PROPR_FILE
  fi


  if [ -n "$DB_PASSWORD" ]; then
    sed -i "s/DB_PASSWORD/${DB_PASSWORD}/g" $PROPR_FILE
  fi

  if [ -n "$DB_RO_USER" ]; then
    sed -i "s/DB_RO_USER/${DB_RO_USER}/g" $PROPR_FILE
  fi

  if [ -n "$DB_RO_PASSWORD" ]; then
    sed -i "s/DB_RO_PASSWORD/${DB_RO_PASSWORD}/g" $PROPR_FILE
  fi

  if [ -n "$SPARQL_ENDPOINT" ]; then
    sed -i "s/SPARQL_ENDPOINT/${SPARQL_ENDPOINT}/g" $PROPR_FILE
  fi

}


replace_propr "$CATALINA_HOME/webapps/data/WEB-INF/classes/*.properties"
replace_propr "$CATALINA_HOME/webapps/data/META-INF/*.xml"
replace_propr "$CR_BASE/build/local.properties"
replace_propr "$CR_BASE/build/target/classes/*.properties"
replace_propr "$CR_BASE/build/target/cr-das/*.properties"

 
exec "$@"

