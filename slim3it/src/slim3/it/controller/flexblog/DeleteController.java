package slim3.it.controller.flexblog;

import org.slim3.controller.JDOController;
import org.slim3.controller.Navigation;

import slim3.it.model.Blog;

public class DeleteController extends JDOController {

    @Override
    public Navigation run() {
        tx.begin();
        Blog blog = pm.getObjectById(Blog.class, requestScope("key"));
        pm.deletePersistent(blog);
        tx.commit();
        return null;
    }
}