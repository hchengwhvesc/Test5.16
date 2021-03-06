package ph.servlet;

import java.io.IOException;


import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.List;
import ph.dao.PetDAO;
import ph.po.Pet;
import ph.dao.UserDAO;
import ph.po.User;
@MultipartConfig


//@WebServlet(name = "PetServlet")
@WebServlet("/PetServlet")
public class PetServlet extends HttpServlet
{
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        //doPost方法会被多个表单调用，查询宠物、增加宠物,因此这里需要根据不同表单传递的标识参数来调用不同的方法
        String m = request.getParameter("m");
        if("search".equals(m))
        {
            searchPet(request, response);
        }
        else if("newPetAdd".equals(m))
        {
            newPetAdd_doPost(request, response);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String m = request.getParameter("m");
        if("deletePet".equals(m))
        {
            deletePet(request, response);
        }
        else if("newPetAdd".equals(m))
        {
            newPetAdd_doGet(request, response);
        }
    }

    //查询宠物的处理逻辑，by hlzhang 20180420
    private void searchPet(HttpServletRequest request,HttpServletResponse response)throws ServletException, IOException
    {
        String petName = request.getParameter("petName");
        String ownerName = request.getParameter("ownerName");
        try
        {
            List<Pet> pets = new PetDAO().search(petName, ownerName);
            if(0==pets.size())
            {
                request.setAttribute("msg", "没有查到宠物信息");
                request.getRequestDispatcher("/petSearch.jsp").forward(request, response);
            }
            else
            {
                request.setAttribute("pets", pets);
                request.getRequestDispatcher("/petSearchResult.jsp").forward(request, response);
            }
        }
        catch (Exception e)
        {
            request.setAttribute("msg", e.getMessage());
            request.getRequestDispatcher("/petSearch.jsp").forward(request, response);
        }
    }

    //在宠物管理页面执行删除宠物操作 by hlzhang，20180425
    private void deletePet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {
//            new PetDAO().delete(Integer.parseInt(request.getParameter("petId")));
            String strPetId = request.getParameter("petId");
            int petId = Integer.parseInt(strPetId);
            PetDAO petDAO = new PetDAO();
            petDAO.delete(petId);
            request.setAttribute("msg", "成功删除宠物："+request.getParameter("petName"));
            request.getRequestDispatcher("/petSearch.jsp").forward(request, response);
        }
        catch (Exception e)
        {
            request.setAttribute("msg", e.getMessage());
            request.getRequestDispatcher("/petSearch.jsp").forward(request, response);
        }
    }

    //增加宠物的处理逻辑,用于处理点击petSearch.jsp页面的增加宠物超链接后的Get操作，by hlzhang 20180518
    private void newPetAdd_doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {
            UserDAO userDAO = new UserDAO();
            List<User> users = userDAO.getAllCustomer();
            request.setAttribute("users", users);
            request.getRequestDispatcher("/newPetAdd.jsp").forward(request, response);
        }
        catch (Exception e)
        {
            request.setAttribute("msg", e.getMessage());
            request.getRequestDispatcher("/petSearch.jsp").forward(request, response);
        }
    }


    //增加宠物的处理逻辑,用于处理newPetAdd.jsp页面的Post提交操作，by hlzhang 20180131
    private void newPetAdd_doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String name = request.getParameter("name");
        if("".equals(name))//宠物姓名不能为空
        {
            try
            {
                UserDAO userDAO = new UserDAO();
                List<User> users = userDAO.getAllCustomer();
                request.setAttribute("users", users);
                request.setAttribute("msg", "请输入宠物姓名");
                request.getRequestDispatcher("/newPetAdd.jsp").forward(request, response);//这里可以直接转发到 newPetAdd.jsp
            }
            catch (Exception e)
            {
                request.setAttribute("msg", e.getMessage());
                request.getRequestDispatcher("/petSearch.jsp").forward(request, response);
            }
            return;//跳转到 newPetAdd.jsp页面后，本函数直接退出
        }
        Part p = request.getPart("photo");
        String filename = getFileName(p);
        //这里需要上传文件，就需要得到上传文件的目标路径，这里的文件保存路径不能是任意路径，只能放到当前应用根目录及其子目录下，才能够通过浏览器访问，如何得到这个当前应用根目录，一般情况下不能写死路径，应该通过代码动态得到路径
        String photo = "photo/default.jpg";
        if(!filename.equals(""))//如果filename文件名不为空表示用户上传了照片，则需要保存照片，否则可以省略这个步骤
        {
            String type = filename.substring(filename.lastIndexOf("."));//xxxx.xx
            String newname = System.currentTimeMillis() + type;
            //System.out.println(getRuntimePath());//路径有了差文件名，如果使用原文件名，可能出现重名文件，若非要使用原文件名，则可以分文件夹或将文件名改为时间毫秒数
            String saveFile = getRuntimePath() + newname;
            photo = "photo/" + newname;
            p.write(saveFile);//上传文件
        }

        Pet pet = new Pet();
        pet.setName(request.getParameter("name"));
        pet.setBirthdate(request.getParameter("birthdate"));
        pet.setOwnerId(Integer.parseInt(request.getParameter("userId")));
        pet.setPhoto(photo);
        try
        {
            new PetDAO().save(pet);
            request.setAttribute("msg", "添加成功");
            request.getRequestDispatcher("/petSearch.jsp").forward(request, response);
        }
        catch (Exception e)
        {
            request.setAttribute("msg",e.getMessage());
            request.getRequestDispatcher("/petSearch.jsp").forward(request, response);
        }
    }

    private String getFileName(Part part)
    {
        String filename = "";
        String contentDec = part.getHeader("content-disposition");// 获取header信息中的content-disposition，如果为文件，则可以从其中提取出文件名

        Pattern p = Pattern.compile("filename=\".+\"");//  filename="任意个字符"   .任意字符   +表示数量不固定
        Matcher m = p.matcher(contentDec);
        if(m.find())
        {
            String temp=m.group();
            filename=temp.substring(10,temp.length()-1);
        }
        return filename;
    }

    private String getRuntimePath()
    {
        String path = "";
        path = this.getServletContext().getRealPath("/photo");
        path += "\\";
        return path;
    }
}