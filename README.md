# Biu!
* Just for fun and feature demo, no support available.
* Main class bglutil.Biu
* Setup static final CONFIG parameters in bglutil.Biu before compile.
* Must setup ~/.aws/credentials with profile name such as "virginia", "tokyo", "china", "beijing", "global", etc. Go through the source for their usage.
* Code here is simply feature piling up. 
* Other required libraries "AWS SDK for Java 1.10.76", "AWS KCL", "Apache Commons Lang"

# Setup
    1. Add AWS toolkit to Eclipse (if AWS SDK for Java is NOT 1.10.76, download it manually).
    2. Create an AWS Java project.
    3. Link additional source to clone path.
    4. Add AWS KCL library to project build path.
    5. Add Apache Commons Lang library to project build path.
    6. Maybe you need to turn off the Errors for restriction access for compiler.
    7. Go through the source code of bglutil/Biu.java and provide missing values to certain static fields.
    8. Build, export and run bglutil.Biu.
