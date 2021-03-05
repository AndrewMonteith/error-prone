# ./collect_diagnostics.sh <project> "<sequence number> <commit hash>"

# Since error prone reads files per disk, we need a project per scan job.
PROJECT_COPY="$ROOT/java-corpus/$RANDOM"
mkdir "$PROJECT_COPY"
cp -r "$ROOT/java-corpus/$1/." "$PROJECT_COPY"
echo "Created $PROJECT_COPY"

# Collect the diagnostics
pushd $ROOT/error-prone
ROOT=$ROOT mvn test -Dtest=HPCCode#scanCommit test -DfailIfNoTests=false -DprojDir="$PROJECT_COPY" -Dproject="$1" -Dcommit="$2" -DoutputFolder="$ROOT/diagnostics/$1"
popd

# Delete the now not needed project copy
rm -rf "$PROJECT_COPY"