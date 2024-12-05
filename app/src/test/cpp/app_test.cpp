
#include "app.h"
#include <cassert>

int main() {
	sample_object_files_mapping::Greeter greeter;
	assert(greeter.greeting().compare("Hello, World!") == 0);
	return 0;
}
