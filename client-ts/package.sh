#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e
# Treat unset variables as an error when substituting.
set -u
# Pipe commands should fail if any command in the pipe fails.
set -o pipefail

# --- Configuration ---
# Directories where the Gradle tasks generate the NPM projects
FETCH_DIR="build/generate/typescript-fetch"
AXIOS_DIR="build/generate/typescript-axios"
REDUX_DIR="build/generate/typescript-redux"

# Output directory for the packaged .tgz files
PACKAGE_OUTPUT_DIR="build/packages"

# --- Ensure required commands are available ---
if ! command -v npm &> /dev/null
then
    echo "Error: npm command could not be found. Please install Node.js and npm."
    exit 1
fi

if ! command -v pnpm &> /dev/null
then
    echo "Error: pnpm command could not be found. Please install pnpm (e.g., 'npm install -g pnpm' or see https://pnpm.io/installation)."
    exit 1
fi

# --- Ensure source directories exist ---
if [ ! -d "$FETCH_DIR" ] || [ ! -d "$AXIOS_DIR" ] || [ ! -d "$REDUX_DIR" ]; then
  echo "Error: One or more source directories ($FETCH_DIR, $AXIOS_DIR, $REDUX_DIR) not found."
  echo "Please run the Gradle build first (e.g., ./gradlew generateTypescript)."
  exit 1
fi

# --- Create package output directory ---
echo "Creating package output directory: $PACKAGE_OUTPUT_DIR"
mkdir -p "$PACKAGE_OUTPUT_DIR"

# --- Store the original directory ---
ORIGINAL_DIR=$(pwd)

# --- Package each project ---

# Function to install dependencies and package a project
package_project() {
  local project_dir="$1"
  local package_name_pattern="$2"
  local project_name=$(basename "$project_dir") # e.g., typescript-axios

  echo ""
  echo "--- Processing $project_name ---"

  if [ ! -d "$project_dir" ]; then
    echo "Error: Directory $project_dir does not exist."
    return 1
  fi

  echo "Entering $project_dir to install dependencies..."
  # Use pushd/popd for safer directory navigation
  pushd "$project_dir" > /dev/null

  echo "Running 'pnpm install' in $(pwd)..."
  # pnpm install will also trigger the 'prepare' script (npm run build -> tsc)
  pnpm install
  if [ $? -ne 0 ]; then
      echo "Error: 'pnpm install' failed in $project_dir"
      popd > /dev/null # Ensure we popd even on failure before exiting
      return 1
  fi

  echo "Returning to $ORIGINAL_DIR..."
  popd > /dev/null

  echo "Packaging $project_name using 'npm pack'..."
  # Run npm pack targeting the source directory.
  # The .tgz file will be created in the current directory (project root).
  npm pack "$project_dir"
  if [ $? -ne 0 ]; then
      echo "Error: 'npm pack' failed for $project_dir"
      return 1
  fi

  # Check if pack created a file matching the pattern
  # Use find to handle potential version variations robustly
  local package_file
  package_file=$(find . -maxdepth 1 -name "${package_name_pattern}-*.tgz" -print -quit)

  if [ -z "$package_file" ]; then
    echo "Error: Failed to find created package file matching '${package_name_pattern}-*.tgz' in $(pwd)."
    return 1
  fi

  echo "Moving package $package_file to $PACKAGE_OUTPUT_DIR..."
  mv "$package_file" "$PACKAGE_OUTPUT_DIR/"
  if [ $? -ne 0 ]; then
      echo "Error: Failed to move $package_file"
      return 1
  fi

  echo "--- Successfully processed $project_name ---"
}

# Package typescript-fetch
package_project "$FETCH_DIR" "modelix-api-client-ts-fetch" || exit 1

# Package typescript-axios
package_project "$AXIOS_DIR" "modelix-api-client-ts-axios" || exit 1

# Package typescript-redux
package_project "$REDUX_DIR" "modelix-api-client-ts-redux" || exit 1

# --- Completion ---
echo ""
echo "-----------------------------------------------------"
echo "NPM Packaging Complete!"
echo "Packages (.tgz files) are located in: $PACKAGE_OUTPUT_DIR"
echo "-----------------------------------------------------"

exit 0