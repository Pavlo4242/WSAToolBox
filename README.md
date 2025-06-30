# Kevin-WSAToolBox
A Windows Subsystem for Android toolbox.


#name: Build Kotlin with kotlinc

#on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:
    inputs:
        environment:
          description: 'Environment to deploy to'
          required: true
          default: 'staging'


jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout source
      uses: actions/checkout@v4

    - name: Install Kotlin compiler
      run: |
        curl -s https://get.sdkman.io | bash
        source "$HOME/.sdkman/bin/sdkman-init.sh"
        sdk install kotlin

    - name: Compile Kotlin files
      run: |
        mkdir -p out
        find WSATOOL/WSATOOL/src -name "*.kt" > sources.txt
        kotlinc @sources.txt -d out/WSAToolBox.jar
