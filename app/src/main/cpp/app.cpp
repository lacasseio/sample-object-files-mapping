
#include <iostream>
#include <stdlib.h>
#include "app.h"

std::string sample_object_files_mapping::Greeter::greeting() {
	return std::string("Hello, World!");
}

int main () {
	sample_object_files_mapping::Greeter greeter;
	std::cout << greeter.greeting() << std::endl;
	return 0;
}
