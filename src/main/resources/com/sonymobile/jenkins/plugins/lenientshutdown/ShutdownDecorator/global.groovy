f=namespace("lib/form")

f.section(title:_("Lenient Shutdown")) {
    f.entry(title:_("Shutdown message"),
            description:_("This message will be displayed in the header on all " +
                    "pages when lenient shutdown mode is activated"), field: "shutdownMessage") {
        f.textbox()
    }
}
